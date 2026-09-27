"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, ApiError, type Interaction } from "@/lib/api";

export default function LogPage() {
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const [interactions, setInteractions] = useState<Interaction[] | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    api
      .interactions(guildId)
      .then(setInteractions)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
      });
  }, [guildId]);

  if (notFound) {
    return (
      <p className="text-sm text-neutral-400">
        This server hasn&apos;t connected yet.
      </p>
    );
  }

  if (!interactions) return <p className="text-sm text-neutral-400">Loading…</p>;

  if (interactions.length === 0) {
    return (
      <p className="text-sm text-neutral-400">
        No commands recorded yet for this server.
      </p>
    );
  }

  return (
    <table className="w-full border-collapse text-sm">
      <thead>
        <tr className="border-b border-neutral-700 text-left text-xs uppercase text-neutral-400">
          <th className="py-2 pr-2">Received</th>
          <th className="py-2 pr-2">Command</th>
          <th className="py-2 pr-2">User</th>
          <th className="py-2 pr-2">Status</th>
          <th className="py-2 pr-2">Severity</th>
          <th className="py-2">AI summary</th>
        </tr>
      </thead>
      <tbody>
        {interactions.map((i) => (
          <tr key={i.id} className="border-b border-neutral-800 align-top">
            <td className="whitespace-nowrap py-2 pr-2 text-neutral-400">
              {new Date(i.receivedAt).toLocaleString()}
            </td>
            <td className="py-2 pr-2 font-mono">
              {i.commandName ? `/${i.commandName}` : "—"}
            </td>
            <td className="py-2 pr-2">{i.userName ?? "—"}</td>
            <td className="py-2 pr-2">{i.status}</td>
            <td className="py-2 pr-2">{i.severity ?? "—"}</td>
            <td className="py-2">{i.aiSummary ?? "—"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
