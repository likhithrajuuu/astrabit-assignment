"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { api, ApiError, type Me } from "@/lib/api";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";
import { Spinner } from "@/components/Spinner";

function ConnectCallbackInner() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [status, setStatus] = useState<"connecting" | "error">("connecting");
  const [errorMessage, setErrorMessage] = useState("");

  const guildId = searchParams.get("guild_id");
  const discordError = searchParams.get("error");

  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch(() => setMe({ authenticated: false }));
  }, []);

  useEffect(() => {
    if (me === null || !me.authenticated) return;

    if (discordError) {
      setStatus("error");
      setErrorMessage("Discord authorization was cancelled or denied.");
      return;
    }
    if (!guildId) {
      setStatus("error");
      setErrorMessage("Discord didn't send back a server ID. Please try again.");
      return;
    }

    api
      .connectGuild(guildId)
      .then(() => router.replace(`/dashboard/${guildId}/commands`))
      .catch((e) => {
        setStatus("error");
        setErrorMessage(
          e instanceof ApiError
            ? `Failed to finish connecting (status ${e.status}).`
            : "Failed to finish connecting."
        );
      });
  }, [me, guildId, discordError, router]);

  if (me === null) {
    return <Spinner label="Loading…" />;
  }

  if (!me.authenticated) {
    return (
      <div className="card-panel w-full max-w-sm text-center">
        <h1 className="mb-2 text-lg font-semibold text-white">
          Sign in to finish connecting
        </h1>
        <p className="mb-6 text-sm text-neutral-400">
          Discord added the bot to your server — sign in with Google to
          finish setting up its dashboard.
        </p>
        <GoogleSignInButton />
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="card-panel w-full max-w-sm text-center">
        <h1 className="mb-2 text-lg font-semibold text-red-400">
          Couldn&apos;t connect
        </h1>
        <p className="mb-6 text-sm text-neutral-400">{errorMessage}</p>
        <a href="/" className="text-sm font-medium text-indigo-400 hover:text-indigo-300">
          &larr; Back home
        </a>
      </div>
    );
  }

  return (
    <div className="card-panel w-full max-w-sm text-center">
      <Spinner label="Connecting your server…" />
      <p className="mt-2 text-sm text-neutral-500">Just a moment.</p>
    </div>
  );
}

export default function ConnectCallbackPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-neutral-950 px-4 text-neutral-100">
      <Suspense fallback={<Spinner label="Loading…" />}>
        <ConnectCallbackInner />
      </Suspense>
    </main>
  );
}
