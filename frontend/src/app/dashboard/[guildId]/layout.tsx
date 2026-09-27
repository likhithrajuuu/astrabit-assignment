"use client";

import Link from "next/link";
import { usePathname, useParams } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { api, type Me } from "@/lib/api";

export default function DashboardLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch(() => setMe({ authenticated: false }));
  }, []);

  if (me === null) {
    return (
      <div className="min-h-screen bg-neutral-950 p-8 text-sm text-neutral-400">
        Loading…
      </div>
    );
  }

  if (!me.authenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-neutral-950 text-neutral-100">
        <div className="w-96 rounded-lg bg-neutral-900 p-8 text-center">
          <h1 className="mb-4 text-lg font-semibold">Sign in required</h1>
          <p className="mb-4 text-sm text-neutral-400">
            You need to sign in with Google to view this dashboard.
          </p>
          <a
            href={api.googleLoginUrl}
            className="inline-block rounded bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
          >
            Sign in with Google
          </a>
        </div>
      </div>
    );
  }

  const tabs = [
    { href: `/dashboard/${guildId}/commands`, label: "Commands" },
    { href: `/dashboard/${guildId}/rules`, label: "Rules" },
    { href: `/dashboard/${guildId}/log`, label: "Log" },
  ];

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100">
      <div className="mx-auto max-w-4xl p-8">
        <div className="mb-2 text-xs text-neutral-400">
          Signed in as {me.email} &middot;{" "}
          <a href={api.logoutUrl} className="underline">
            Sign out
          </a>
        </div>
        <h1 className="mb-4 text-xl font-bold">
          Astrabot Dashboard &middot; {guildId}
        </h1>
        <nav className="mb-6 flex gap-4 text-sm">
          {tabs.map((tab) => (
            <Link
              key={tab.href}
              href={tab.href}
              className={
                pathname === tab.href
                  ? "font-semibold text-white"
                  : "text-neutral-400 hover:text-white"
              }
            >
              {tab.label}
            </Link>
          ))}
        </nav>
        {children}
      </div>
    </div>
  );
}
