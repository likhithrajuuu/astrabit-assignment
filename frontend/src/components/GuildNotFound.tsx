import { discordInviteUrl } from "@/lib/api";
import { DiscordIcon } from "@/components/icons";

export function GuildNotFound({ guildId }: { guildId: string }) {
  return (
    <div className="card-panel mx-auto max-w-md text-center">
      <h2 className="mb-2 text-lg font-semibold text-white">
        This server isn&apos;t connected yet
      </h2>
      <p className="mb-6 text-sm text-neutral-400">
        No server with ID <code className="rounded bg-neutral-800 px-1.5 py-0.5 font-mono text-neutral-300">{guildId}</code> is
        set up with Astrabot yet. Add the bot to it directly:
      </p>
      <a
        href={discordInviteUrl()}
        className="btn-primary w-full !bg-[#5865F2] hover:!bg-[#4752c4]"
      >
        <DiscordIcon className="h-4 w-4" />
        Add Astrabot to a server
      </a>
    </div>
  );
}
