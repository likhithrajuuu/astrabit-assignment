export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-6 text-sm text-neutral-400">
      <div className="h-5 w-5 animate-spin rounded-full border-2 border-neutral-700 border-t-indigo-500" />
      {label && <span>{label}</span>}
    </div>
  );
}
