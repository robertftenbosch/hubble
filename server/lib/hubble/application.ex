defmodule Hubble.Application do
  @moduledoc false
  use Application

  @impl true
  def start(_type, _args) do
    port = Application.get_env(:hubble, :port, 4000)

    children = [
      {Registry, keys: :duplicate, name: Hubble.Signaling.Registry},
      Hubble.Heatmap.Server,
      Hubble.Relay.Server,
      {Bandit, plug: Hubble.Api.Router, port: port}
    ]

    Supervisor.start_link(children, strategy: :one_for_one, name: Hubble.Supervisor)
  end
end
