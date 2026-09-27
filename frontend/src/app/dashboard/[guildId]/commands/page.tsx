"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, ApiError, type CommandConfig } from "@/lib/api";
import { Toggle } from "@/components/Toggle";
import { Spinner } from "@/components/Spinner";
import { GuildNotFound } from "@/components/GuildNotFound";

export default function CommandsPage() {
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const [configs, setConfigs] = useState<CommandConfig[] | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    api
      .commands(guildId)
      .then(setConfigs)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
      });
  }, [guildId]);

  function update(index: number, patch: Partial<CommandConfig>) {
    setSaved(false);
    setConfigs((prev) =>
      prev ? prev.map((c, i) => (i === index ? { ...c, ...patch } : c)) : prev
    );
  }

  async function save() {
    if (!configs) return;
    setSaving(true);
    try {
      await api.saveCommands(guildId, configs);
      setSaved(true);
    } finally {
      setSaving(false);
    }
  }

  if (notFound) return <GuildNotFound guildId={guildId} />;
  if (!configs) return <Spinner label="Loading commands…" />;

  return (
    <div>
      <div className="mb-5 flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Commands</h2>
          <p className="text-sm text-neutral-400">
            Turn commands on or off, and customize how they respond.
          </p>
        </div>
        <button onClick={save} disabled={saving} className="btn-primary">
          {saving ? "Saving…" : saved ? "Saved ✓" : "Save all changes"}
        </button>
      </div>

      <div className="card-panel overflow-x-auto !p-0">
        <table className="data-table">
          <thead>
            <tr>
              <th className="pl-5">Command</th>
              <th>Enabled</th>
              <th>Mirror</th>
              <th>AI</th>
              <th>Ephemeral</th>
              <th className="pr-5">Reply template</th>
            </tr>
          </thead>
          <tbody>
            {configs.map((c, i) => (
              <tr key={c.commandName}>
                <td className="pl-5 font-mono text-neutral-200">
                  /{c.commandName}
                </td>
                <td>
                  <Toggle
                    checked={c.enabled}
                    onChange={(v) => update(i, { enabled: v })}
                    label={`Enable /${c.commandName}`}
                  />
                </td>
                <td>
                  <Toggle
                    checked={c.mirrorEnabled}
                    onChange={(v) => update(i, { mirrorEnabled: v })}
                    label={`Mirror /${c.commandName}`}
                  />
                </td>
                <td>
                  <Toggle
                    checked={c.aiEnabled}
                    onChange={(v) => update(i, { aiEnabled: v })}
                    label={`AI for /${c.commandName}`}
                  />
                </td>
                <td>
                  <Toggle
                    checked={c.ephemeral}
                    onChange={(v) => update(i, { ephemeral: v })}
                    label={`Ephemeral /${c.commandName}`}
                  />
                </td>
                <td className="pr-5">
                  <input
                    type="text"
                    value={c.replyTemplate ?? ""}
                    onChange={(e) => update(i, { replyTemplate: e.target.value })}
                    placeholder="(default)"
                    className="input-field !py-1.5"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
