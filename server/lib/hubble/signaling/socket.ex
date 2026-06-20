defmodule Hubble.Signaling.Socket do
  @moduledoc """
  WebSock handler for a peer's signaling connection (`/signal/:id`).

  Inbound text frames are JSON: `{"to": "<peerId>", "type": "offer|answer|ice",
  "payload": <any>}`. The server stamps `from` (this socket's id) and routes the
  message to the target peer's socket. Outbound `{:signal, msg}` info messages are
  pushed to this socket as JSON.
  """
  @behaviour WebSock

  @impl true
  def init(%{id: id} = state) do
    Hubble.Signaling.register(id)
    {:ok, state}
  end

  @impl true
  def handle_in({text, [opcode: :text]}, %{id: id} = state) do
    case Jason.decode(text) do
      {:ok, %{"to" => to} = msg} ->
        Hubble.Signaling.deliver(to, %{
          "from" => id,
          "type" => Map.get(msg, "type"),
          "payload" => Map.get(msg, "payload")
        })

        {:ok, state}

      _ ->
        {:reply, :ok, {:text, Jason.encode!(%{"error" => "bad signaling message"})}, state}
    end
  end

  @impl true
  def handle_in(_other, state), do: {:ok, state}

  @impl true
  def handle_info({:signal, message}, state) do
    {:push, {:text, Jason.encode!(message)}, state}
  end

  @impl true
  def handle_info(_msg, state), do: {:ok, state}

  @impl true
  def terminate(_reason, state), do: {:ok, state}
end
