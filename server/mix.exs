defmodule Hubble.MixProject do
  use Mix.Project

  # Discovery server for Hubble. This first slice is a plain OTP app holding the
  # privacy-preserving heatmap core (no DB, no network deps) so it is fully
  # unit-testable. The Phoenix endpoint + channels (beacon ingest, heatmap query,
  # later WebRTC signaling/relay) are layered on top in a later phase.

  # Version is auto-derived at compile time: base from /VERSION (one above this dir),
  # alpha counter from `git rev-list --count HEAD`. Same scheme as the Kotlin modules,
  # so phone/desktop/server can all be correlated to the same commit. SemVer-strict
  # form (no +sha) — Hex/SemVer don't allow build metadata in :version.
  @version_base "../VERSION" |> Path.expand(__DIR__) |> File.read!() |> String.trim()
  @commit_count (case System.cmd("git", ["rev-list", "--count", "HEAD"], stderr_to_stdout: true) do
                   {out, 0} -> String.trim(out)
                   _ -> "0"
                 end)

  def project do
    [
      app: :hubble,
      version: "#{@version_base}.#{@commit_count}",
      elixir: "~> 1.17",
      start_permanent: Mix.env() == :prod,
      deps: deps()
    ]
  end

  def application do
    [
      extra_applications: [:logger],
      mod: {Hubble.Application, []}
    ]
  end

  defp deps do
    [
      {:bandit, "~> 1.5"},
      {:plug, "~> 1.16"},
      {:websock_adapter, "~> 0.5"},
      {:jason, "~> 1.4"}
    ]
  end
end
