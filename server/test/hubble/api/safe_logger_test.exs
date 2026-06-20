defmodule Hubble.Api.SafeLoggerTest do
  use ExUnit.Case, async: false
  use Plug.Test
  import ExUnit.CaptureLog

  @opts Hubble.Api.Router.init([])

  defp call(conn), do: Hubble.Api.Router.call(conn, @opts)

  test "redact/1 collapses opaque path segments" do
    assert Hubble.Api.SafeLogger.redact("/mailbox/box123") == "/mailbox/_"
    assert Hubble.Api.SafeLogger.redact("/signal/peer-abc") == "/signal/_"
    assert Hubble.Api.SafeLogger.redact("/health") == "/health"
    assert Hubble.Api.SafeLogger.redact("/heatmap") == "/heatmap"
  end

  test "a real mailbox request never leaks the id to the access log" do
    secret = "supersecret-mailbox-id-#{System.unique_integer([:positive])}"
    env = Base.encode64(<<9, 8, 7>>)

    log =
      capture_log(fn ->
        call(
          conn(:post, "/mailbox/#{secret}", Jason.encode!(%{envelope: env}))
          |> put_req_header("content-type", "application/json")
        )

        call(conn(:get, "/mailbox/#{secret}"))
      end)

    refute log =~ secret, "mailbox id leaked into the log: #{log}"
    assert log =~ "/mailbox/_"
  end
end
