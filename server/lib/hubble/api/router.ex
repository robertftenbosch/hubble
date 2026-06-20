defmodule Hubble.Api.Router do
  @moduledoc """
  HTTP API for the Hubble discovery + relay server.

    POST /beacon        {"geohash": "u15hy7"}        -> 204  (anonymous presence)
    GET  /heatmap                                    -> 200  [{"cell","count"}, ...]
    POST /mailbox/:id   {"envelope": "<base64>"}      -> 204  (store opaque envelope)
    GET  /mailbox/:id                                -> 200  {"envelopes": ["<base64>", ...]}
    GET  /health                                     -> 200  {"ok": true}

  Everything is opaque to the server: beacons carry no identity, and envelopes are
  E2E-encrypted blobs routed only by an unlinkable mailbox id.
  """
  use Plug.Router

  plug(Hubble.Api.SafeLogger)
  plug(:match)
  plug(Plug.Parsers, parsers: [:json], pass: ["application/json"], json_decoder: Jason)
  plug(:dispatch)

  get "/health" do
    version = Application.spec(:hubble, :vsn) |> to_string()
    send_json(conn, 200, %{ok: true, version: version})
  end

  # WebRTC signaling: upgrade to a WebSocket bound to this peer's id.
  get "/signal/:id" do
    conn
    |> WebSockAdapter.upgrade(Hubble.Signaling.Socket, %{id: id}, timeout: 120_000)
    |> halt()
  end

  post "/beacon" do
    case conn.body_params do
      %{"geohash" => geohash} when is_binary(geohash) ->
        Hubble.Heatmap.Server.record(geohash)
        send_resp(conn, 204, "")

      _ ->
        send_json(conn, 400, %{error: "geohash required"})
    end
  end

  get "/heatmap" do
    cells = Hubble.Heatmap.Server.heatmap()
    send_json(conn, 200, cells)
  end

  post "/mailbox/:id" do
    case conn.body_params do
      %{"envelope" => b64} when is_binary(b64) ->
        case Base.decode64(b64) do
          {:ok, bytes} ->
            Hubble.Relay.Server.deposit(id, bytes)
            send_resp(conn, 204, "")

          :error ->
            send_json(conn, 400, %{error: "envelope must be base64"})
        end

      _ ->
        send_json(conn, 400, %{error: "envelope required"})
    end
  end

  get "/mailbox/:id" do
    envelopes = Hubble.Relay.Server.collect(id) |> Enum.map(&Base.encode64/1)
    send_json(conn, 200, %{envelopes: envelopes})
  end

  match _ do
    send_json(conn, 404, %{error: "not found"})
  end

  defp send_json(conn, status, body) do
    conn
    |> put_resp_content_type("application/json")
    |> send_resp(status, Jason.encode!(body))
  end
end
