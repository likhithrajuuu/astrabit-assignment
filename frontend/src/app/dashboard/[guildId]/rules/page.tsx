"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { api, ApiError, type Meta, type Rule } from "@/lib/api";
import { Spinner } from "@/components/Spinner";
import { GuildNotFound } from "@/components/GuildNotFound";

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
  const [adding, setAdding] = useState(false);

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
    setAdding(true);
    try {
      const created = await api.addRule(guildId, draft);
      setRules((prev) => (prev ? [...prev, created] : [created]));
      setDraft(emptyRule(meta));
    } finally {
      setAdding(false);
    }
  }

  async function removeRule(ruleId: number | null) {
    if (ruleId == null) return;
    await api.deleteRule(guildId, ruleId);
    setRules((prev) => prev?.filter((r) => r.id !== ruleId) ?? null);
  }

  if (notFound) return <GuildNotFound guildId={guildId} />;
  if (!rules || !meta || !draft) return <Spinner label="Loading rules…" />;

  return (
    <div>
      <div className="mb-5">
        <h2 className="text-lg font-semibold text-white">Rules</h2>
        <p className="text-sm text-neutral-400">
          Match report/report-like content and trigger an action automatically.
        </p>
      </div>

      {rules.length === 0 ? (
        <div className="card-panel mb-6 text-center text-sm text-neutral-400">
          No rules yet for this server — add one below.
        </div>
      ) : (
        <div className="card-panel mb-6 overflow-x-auto !p-0">
          <table className="data-table">
            <thead>
              <tr>
                <th className="pl-5">Priority</th>
                <th>Command</th>
                <th>Match type</th>
                <th>Pattern</th>
                <th>Action</th>
                <th>Params</th>
                <th className="pr-5" />
              </tr>
            </thead>
            <tbody>
              {rules.map((r) => (
                <tr key={r.id}>
                  <td className="pl-5 text-neutral-300">{r.priority}</td>
                  <td className="font-mono text-neutral-200">/{r.commandName}</td>
                  <td>
                    <span className="badge bg-neutral-800 text-neutral-300">
                      {r.matchType}
                    </span>
                  </td>
                  <td className="max-w-[16ch] truncate text-neutral-300" title={r.pattern}>
                    {r.pattern}
                  </td>
                  <td>
                    <span className="badge bg-indigo-600/15 text-indigo-300">
                      {r.action}
                    </span>
                  </td>
                  <td className="max-w-[16ch] truncate font-mono text-xs text-neutral-500" title={r.params}>
                    {r.params}
                  </td>
                  <td className="pr-5">
                    <button onClick={() => removeRule(r.id)} className="btn-danger">
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="card-panel">
        <h3 className="mb-4 text-sm font-semibold text-neutral-200">Add a rule</h3>
        <form onSubmit={addRule} className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          <div>
            <label className="field-label">Command</label>
            <select
              value={draft.commandName}
              onChange={(e) => setDraft({ ...draft, commandName: e.target.value })}
              className="input-field"
            >
              {meta.commandNames.map((name) => (
                <option key={name} value={name}>
                  /{name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">Match type</label>
            <select
              value={draft.matchType}
              onChange={(e) =>
                setDraft({ ...draft, matchType: e.target.value as Rule["matchType"] })
              }
              className="input-field"
            >
              {meta.matchTypes.map((mt) => (
                <option key={mt} value={mt}>
                  {mt}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">Pattern</label>
            <input
              required
              value={draft.pattern}
              onChange={(e) => setDraft({ ...draft, pattern: e.target.value })}
              placeholder="e.g. spam or a regex"
              className="input-field"
            />
          </div>
          <div>
            <label className="field-label">Action</label>
            <select
              value={draft.action}
              onChange={(e) =>
                setDraft({ ...draft, action: e.target.value as Rule["action"] })
              }
              className="input-field"
            >
              {meta.actions.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="field-label">Params (JSON)</label>
            <input
              value={draft.params}
              onChange={(e) => setDraft({ ...draft, params: e.target.value })}
              placeholder="{}"
              className="input-field"
            />
          </div>
          <div>
            <label className="field-label">Priority</label>
            <input
              type="number"
              value={draft.priority}
              onChange={(e) => setDraft({ ...draft, priority: Number(e.target.value) })}
              className="input-field"
            />
          </div>
          <div className="col-span-full">
            <button type="submit" disabled={adding} className="btn-primary">
              {adding ? "Adding…" : "Add rule"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
