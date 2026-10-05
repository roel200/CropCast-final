// Cloudflare Worker: keeps the LLM API keys off the phone and tries providers in order.
// Secrets: OPENROUTER_KEY, XKIRO_KEY. Vars: see wrangler.toml.

const SYSTEM = (language) =>
  `You are an agronomy assistant for Philippine smallholder farmers. Explain the crop recommendation facts you are given in plain, short sentences, in ${language}. ` +
  "Use only the numbers and crops in the facts. Never invent crops, rates or prices. Say what to plant, why it ranks first, and the top 2-3 things to do before planting. " +
  "No markdown, max 150 words.";

const MAX_FACTS = 4000;

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

async function ask(url, key, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${key}` },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(20000),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  const text = data.choices?.[0]?.message?.content?.replace(/<think>[\s\S]*?<\/think>/g, "").trim();
  if (!text) throw new Error("empty reply");
  return text;
}

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ error: "POST only" }, 405);

    // ponytail: per-IP limit only if a ratelimit binding named LIMITER is configured; add one before public release.
    if (env.LIMITER) {
      const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
      if (!(await env.LIMITER.limit({ key: ip })).success) return json({ error: "Too many requests" }, 429);
    }

    let facts, language;
    try {
      ({ facts, language } = await request.json());
    } catch {
      return json({ error: "Bad JSON" }, 400);
    }
    if (typeof facts !== "string" || !facts.trim() || facts.length > MAX_FACTS) return json({ error: "Bad facts" }, 400);
    const lang = { Hiligaynon: "Hiligaynon", Filipino: "Filipino (Tagalog)" }[language] ?? "English";

    const messages = [
      { role: "system", content: SYSTEM(lang) },
      { role: "user", content: facts },
    ];
    const common = { messages, max_tokens: 500, temperature: 0.3 };
    const attempts = [
      // OpenRouter tries `models` in order itself; xKiro is the second provider.
      env.OPENROUTER_KEY && (() =>
        ask("https://openrouter.ai/api/v1/chat/completions", env.OPENROUTER_KEY, {
          ...common,
          models: [env.OPENROUTER_MODEL, env.OPENROUTER_MODEL_2].filter(Boolean),
        })),
      env.XKIRO_KEY && env.XKIRO_URL && (() =>
        // Accept either the base URL (.../v1) or the full chat-completions URL.
        ask(env.XKIRO_URL.replace(/\/+$/, "").replace(/(\/chat\/completions)?$/, "/chat/completions"),
          env.XKIRO_KEY, { ...common, model: env.XKIRO_MODEL })),
    ].filter(Boolean);

    for (const attempt of attempts) {
      try {
        return json({ text: await attempt() });
      } catch {
        // fall through to the next provider
      }
    }
    return json({ error: "All providers failed" }, 502);
  },
};
