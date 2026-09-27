"use client";

import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { api, type Me } from "@/lib/api";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";
import { CommandIcon, RuleIcon, LogIcon } from "@/components/icons";
import { Spinner } from "@/components/Spinner";

export default function DashboardLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const params = useParams<{ guildId: string }>();
  const guildId = params.guildId;
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch(() => setMe({ authenticated: false }));
  }, []);

  async function signOut() {
    await api.logout();
    router.push("/");
  }

  if (me === null) {
    return (
      <div className="flex min-h-screen items-center justify-center text-neutral-100">
        <Spinner label="Loading…" />
      </div>
    );
  }

  if (!me.authenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center px-4 text-neutral-100">
        <div className="card-panel w-full max-w-sm text-center">
          <h1 className="mb-2 text-lg font-semibold text-white">
            Sign in required
          </h1>
          <p className="mb-6 text-sm text-neutral-400">
            You need to sign in with Google to view this dashboard.
          </p>
          <GoogleSignInButton />
        </div>
      </div>
    );
  }

  const tabs = [
    { href: `/dashboard/${guildId}/commands`, label: "Commands", icon: CommandIcon },
    { href: `/dashboard/${guildId}/rules`, label: "Rules", icon: RuleIcon },
    { href: `/dashboard/${guildId}/log`, label: "Log", icon: LogIcon },
  ];

  return (
    <div className="min-h-screen text-neutral-100">
      <header className="border-b border-neutral-800/80 bg-neutral-950/60 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div>
            <p className="text-xs font-medium text-neutral-500">
              Astrabot Dashboard
            </p>
            <h1 className="font-mono text-sm font-semibold text-white">
              {guildId}
            </h1>
          </div>
          <div className="flex items-center gap-4">
            <Link href="/" className="text-xs font-medium text-neutral-400 hover:text-neutral-200">
              Switch server
            </Link>
            <span className="text-neutral-700">|</span>
            <span className="text-xs text-neutral-400">{me.email}</span>
            <button
              onClick={signOut}
              className="text-xs font-medium text-neutral-500 hover:text-neutral-300"
            >
              Sign out
            </button>
          </div>
        </div>
        <div className="mx-auto flex max-w-5xl gap-1 px-6 pb-3">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const active = pathname === tab.href;
            return (
              <Link
                key={tab.href}
                href={tab.href}
                className={active ? "nav-tab-active" : "nav-tab"}
              >
                <Icon />
                {tab.label}
              </Link>
            );
          })}
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">{children}</main>
    </div>
  );
}
