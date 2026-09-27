"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, ApiError, type CommandConfig } from "@/lib/api";

export default function CommandsPage() {
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const [configs, setConfigs] = useState<CommandConfig[] | null>(null);
  const [notFound, setNotFound] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api
      .commands(guildId)
      .then(setConfigs)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
      });
  }, [guildId]);

  function update(index: number, patch: Partial<CommandConfig>) {
    setConfigs((prev) =>
      prev ? prev.map((c, i) => (i === index ? { ...c, ...patch } : c)) : prev
    );
  }

  async function save() {
    if (!configs) return;
    setSaving(true);
    try {
      await api.saveCommands(guildId, configs);
    } finally {
      setSaving(false);
    }
  }

  if (notFound) {
    return (
      <p className="text-sm text-neutral-400">
        This server hasn&apos;t connected yet — run any command (e.g. /ping)
        in that Discord server first, then reload this page.
      </p>
    );
  }

  if (!configs) return <p className="text-sm text-neutral-400">Loading…</p>;

  return (
    <div>
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b border-neutral-700 text-left text-xs uppercase text-neutral-400">
            <th className="py-2 pr-2">Command</th>
            <th className="py-2 pr-2">Enabled</th>
            <th className="py-2 pr-2">Mirror</th>
            <th className="py-2 pr-2">AI</th>
            <th className="py-2 pr-2">Ephemeral</th>
            <th className="py-2">Reply template</th>
          </tr>
        </thead>
        <tbody>
          {configs.map((c, i) => (
            <tr key={c.commandName} className="border-b border-neutral-800">
              <td className="py-2 pr-2 font-mono">/{c.commandName}</td>
              <td className="py-2 pr-2">
                <input
                  type="checkbox"
                  checked={c.enabled}
                  onChange={(e) => update(i, { enabled: e.target.checked })}
                />
              </td>
              <td className="py-2 pr-2">
                <input
                  type="checkbox"
                  checked={c.mirrorEnabled}
                  onChange={(e) =>
                    update(i, { mirrorEnabled: e.target.checked })
                  }
                />
              </td>
              <td className="py-2 pr-2">
                <input
                  type="checkbox"
                  checked={c.aiEnabled}
                  onChange={(e) => update(i, { aiEnabled: e.target.checked })}
                />
              </td>
              <td className="py-2 pr-2">
                <input
                  type="checkbox"
                  checked={c.ephemeral}
                  onChange={(e) => update(i, { ephemeral: e.target.checked })}
                />
              </td>
              <td className="py-2">
                <input
                  type="text"
                  value={c.replyTemplate ?? ""}
                  onChange={(e) =>
                    update(i, { replyTemplate: e.target.value })
                  }
                  placeholder="(default)"
                  className="w-full rounded border border-neutral-700 bg-neutral-800 px-2 py-1"
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <button
        onClick={save}
        disabled={saving}
        className="mt-4 rounded bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        {saving ? "Saving…" : "Save all changes"}
      </button>
    </div>
  );
}
