"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { api, ApiError, type Page, type Interaction } from "@/lib/api";
import { Spinner } from "@/components/Spinner";
import { GuildNotFound } from "@/components/GuildNotFound";

const PAGE_SIZE = 20;

const STATUS_STYLES: Record<string, string> = {
  RECEIVED: "bg-neutral-800 text-neutral-300",
  PROCESSING: "bg-amber-500/15 text-amber-300",
  PROCESSED: "bg-emerald-500/15 text-emerald-300",
  FAILED: "bg-red-500/15 text-red-300",
};

function severityStyle(severity: string | null): string {
  if (!severity) return "bg-neutral-800 text-neutral-400";
  if (severity.includes("5") || severity.toLowerCase().includes("critical")) {
    return "bg-red-500/15 text-red-300";
  }
  if (severity.includes("4") || severity.toLowerCase().includes("high")) {
    return "bg-orange-500/15 text-orange-300";
  }
  if (severity.includes("3") || severity.toLowerCase().includes("moderate")) {
    return "bg-amber-500/15 text-amber-300";
  }
  return "bg-emerald-500/15 text-emerald-300";
}

export default function LogPage() {
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const [pageIndex, setPageIndex] = useState(0);
  const [data, setData] = useState<Page<Interaction> | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    setData(null);
    api
      .interactions(guildId, pageIndex, PAGE_SIZE)
      .then(setData)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) setNotFound(true);
      });
  }, [guildId, pageIndex]);

  if (notFound) return <GuildNotFound guildId={guildId} />;
  if (!data) return <Spinner label="Loading log…" />;

  const { content: interactions, totalElements, first, last } = data;

  return (
    <div>
      <div className="mb-5 flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Activity log</h2>
          <p className="text-sm text-neutral-400">
            Every command this server has run, and what Astrabot did about it.
          </p>
        </div>
        {totalElements > 0 && (
          <p className="text-xs text-neutral-500">
            {totalElements} total
          </p>
        )}
      </div>

      {interactions.length === 0 ? (
        <div className="card-panel text-center text-sm text-neutral-400">
          No commands recorded yet for this server.
        </div>
      ) : (
        <>
          <div className="card-panel overflow-x-auto !p-0">
            <table className="data-table">
              <thead>
                <tr>
                  <th className="pl-5">Received</th>
                  <th>Command</th>
                  <th>User</th>
                  <th>Status</th>
                  <th>Severity</th>
                  <th className="pr-5">AI summary</th>
                </tr>
              </thead>
              <tbody>
                {interactions.map((i) => (
                  <tr key={i.id}>
                    <td className="pl-5 whitespace-nowrap text-neutral-500">
                      {new Date(i.receivedAt).toLocaleString()}
                    </td>
                    <td className="font-mono text-neutral-200">
                      {i.commandName ? `/${i.commandName}` : "—"}
                    </td>
                    <td className="text-neutral-300">{i.userName ?? "—"}</td>
                    <td>
                      <span className={`badge ${STATUS_STYLES[i.status] ?? "bg-neutral-800 text-neutral-300"}`}>
                        {i.status}
                      </span>
                    </td>
                    <td>
                      {i.severity ? (
                        <span className={`badge ${severityStyle(i.severity)}`}>{i.severity}</span>
                      ) : (
                        <span className="text-neutral-600">—</span>
                      )}
                    </td>
                    <td className="pr-5 max-w-[32ch] truncate text-neutral-400" title={i.aiSummary ?? undefined}>
                      {i.aiSummary ?? "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 flex items-center justify-between">
            <button
              onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
              disabled={first}
              className="btn-secondary !px-3 !py-1.5 text-xs"
            >
              &larr; Newer
            </button>
            <span className="text-xs text-neutral-500">
              Page {pageIndex + 1} of {data.totalPages}
            </span>
            <button
              onClick={() => setPageIndex((p) => p + 1)}
              disabled={last}
              className="btn-secondary !px-3 !py-1.5 text-xs"
            >
              Older &rarr;
            </button>
          </div>
        </>
      )}
    </div>
  );
}
