defmodule Hubble.Signaling do
  @moduledoc """
  Routing for WebRTC signaling. Peers connect a WebSocket under their signaling id
  (the same opaque mailbox id used by the relay) and exchange `offer`/`answer`/`ice`
  messages to negotiate a direct P2P data channel. The server only relays opaque
  signaling payloads between connected peers — it never inspects SDP/ICE contents
  beyond addressing, and holds nothing once peers disconnect.

  Backed by a duplicate `Registry` (`Hubble.Signaling.Registry`) keyed by peer id.
  """

  @registry Hubble.Signaling.Registry

  @doc "Register the calling process as the socket for [id]. Auto-unregistered on exit."
  def register(id) when is_binary(id) do
    Registry.register(@registry, id, nil)
  end

  @doc "Deliver a signaling message to every socket currently registered under [to]."
  def deliver(to, message) when is_binary(to) do
    Registry.dispatch(@registry, to, fn entries ->
      for {pid, _} <- entries, do: send(pid, {:signal, message})
    end)

    :ok
  end
end
