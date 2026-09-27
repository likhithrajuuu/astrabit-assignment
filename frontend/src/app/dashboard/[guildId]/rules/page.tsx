"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { api, ApiError, type Meta, type Rule } from "@/lib/api";

const emptyRule = (meta: Meta): Partial<Rule> => ({
  commandName: meta.commandNames[0],
  matchType: meta.matchTypes[0],
  pattern: "",
  action: meta.actions[0],
  params: "{}",
  priority: 0,
});

export default function RulesPage() {
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const [rules, setRules] = useState<Rule[] | null>(null);
  const [meta, setMeta] = useState<Meta | null>(null);
  const [draft, setDraft] = useState<Partial<Rule> | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    api.meta().then((m) => {
      setMeta(m);
      setDraft(emptyRule(m));
    });
    api
      .rules(guildId)
      .then(setRules)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
      });
  }, [guildId]);

  async function addRule(e: FormEvent) {
    e.preventDefault();
    if (!draft || !meta) return;
    const created = await api.addRule(guildId, draft);
    setRules((prev) => (prev ? [...prev, created] : [created]));
    setDraft(emptyRule(meta));
  }

  async function removeRule(ruleId: number | null) {
    if (ruleId == null) return;
    await api.deleteRule(guildId, ruleId);
    setRules((prev) => prev?.filter((r) => r.id !== ruleId) ?? null);
  }

  if (notFound) {
    return (
      <p className="text-sm text-neutral-400">
        This server hasn&apos;t connected yet.
      </p>
    );
  }

  if (!rules || !meta || !draft)
    return <p className="text-sm text-neutral-400">Loading…</p>;

  return (
    <div>
      {rules.length === 0 && (
        <p className="text-sm text-neutral-400">
          No rules yet for this server.
        </p>
      )}
      {rules.length > 0 && (
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-neutral-700 text-left text-xs uppercase text-neutral-400">
              <th className="py-2 pr-2">Priority</th>
              <th className="py-2 pr-2">Command</th>
              <th className="py-2 pr-2">Match type</th>
              <th className="py-2 pr-2">Pattern</th>
              <th className="py-2 pr-2">Action</th>
              <th className="py-2 pr-2">Params</th>
              <th className="py-2" />
            </tr>
          </thead>
          <tbody>
            {rules.map((r) => (
              <tr key={r.id} className="border-b border-neutral-800">
                <td className="py-2 pr-2">{r.priority}</td>
                <td className="py-2 pr-2 font-mono">/{r.commandName}</td>
                <td className="py-2 pr-2">{r.matchType}</td>
                <td className="py-2 pr-2">{r.pattern}</td>
                <td className="py-2 pr-2">{r.action}</td>
                <td className="py-2 pr-2">{r.params}</td>
                <td className="py-2">
                  <button
                    onClick={() => removeRule(r.id)}
                    className="rounded bg-red-600 px-3 py-1 text-xs font-semibold text-white hover:bg-red-700"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2 className="mt-8 mb-2 text-sm font-semibold text-neutral-300">
        Add a rule
      </h2>
      <form onSubmit={addRule} className="grid grid-cols-6 items-end gap-2">
        <div>
          <label className="mb-1 block text-xs text-neutral-400">
            Command
          </label>
          <select
            value={draft.commandName}
            onChange={(e) =>
              setDraft({ ...draft, commandName: e.target.value })
            }
            className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1 text-sm"
          >
            {meta.commandNames.map((name) => (
              <option key={name} value={name}>
                /{name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs text-neutral-400">
            Match type
          </label>
          <select
            value={draft.matchType}
            onChange={(e) =>
              setDraft({
                ...draft,
                matchType: e.target.value as Rule["matchType"],
              })
            }
            className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1 text-sm"
          >
            {meta.matchTypes.map((mt) => (
              <option key={mt} value={mt}>
                {mt}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs text-neutral-400">
            Pattern
          </label>
          <input
            required
            value={draft.pattern}
            onChange={(e) => setDraft({ ...draft, pattern: e.target.value })}
            placeholder="e.g. spam or a regex"
            className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-neutral-400">
            Action
          </label>
          <select
            value={draft.action}
            onChange={(e) =>
              setDraft({ ...draft, action: e.target.value as Rule["action"] })
            }
            className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1 text-sm"
          >
            {meta.actions.map((a) => (
              <option key={a} value={a}>
                {a}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="mb-1 block text-xs text-neutral-400">
            Params (JSON)
          </label>
          <input
            value={draft.params}
            onChange={(e) => setDraft({ ...draft, params: e.target.value })}
            placeholder="{}"
            className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-neutral-400">
            Priority
          </label>
          <input
            type="number"
            value={draft.priority}
            onChange={(e) =>
              setDraft({ ...draft, priority: Number(e.target.value) })
            }
            className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1 text-sm"
          />
        </div>
        <button
          type="submit"
          className="col-span-6 mt-2 w-fit rounded bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          Add rule
        </button>
      </form>
    </div>
  );
}
