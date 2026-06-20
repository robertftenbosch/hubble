defmodule Hubble.MixProject do
  use Mix.Project

  # Discovery server for Hubble. This first slice is a plain OTP app holding the
  # privacy-preserving heatmap core (no DB, no network deps) so it is fully
  # unit-testable. The Phoenix endpoint + channels (beacon ingest, heatmap query,
  # later WebRTC signaling/relay) are layered on top in a later phase.
  def project do
    [
      app: :hubble,
      version: "0.1.0",
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
