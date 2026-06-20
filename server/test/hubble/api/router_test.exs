defmodule Hubble.Api.RouterTest do
  use ExUnit.Case, async: false
  use Plug.Test

  @opts Hubble.Api.Router.init([])

  # The named Heatmap/Relay servers are started by Hubble.Application when the app
  # boots for the test run; the router calls them by name.

  defp call(conn), do: Hubble.Api.Router.call(conn, @opts)

  test "GET /health" do
    conn = call(conn(:get, "/health"))
    assert conn.status == 200
    assert Jason.decode!(conn.resp_body) == %{"ok" => true}
  end

  test "beacons feed the heatmap (k-anonymity respected)" do
    for _ <- 1..3 do
      assert call(conn(:post, "/beacon", Jason.encode!(%{geohash: "u15hy7aa"}))
                  |> put_req_header("content-type", "application/json")).status == 204
    end

    conn = call(conn(:get, "/heatmap"))
    assert conn.status == 200
    cells = Jason.decode!(conn.resp_body)
    assert Enum.any?(cells, &(&1["cell"] == "u15hy7" and &1["count"] == 3))
  end

  test "a lone beacon stays suppressed" do
    call(conn(:post, "/beacon", Jason.encode!(%{geohash: "z9z9z9"}))
         |> put_req_header("content-type", "application/json"))

    conn = call(conn(:get, "/heatmap"))
    refute Enum.any?(Jason.decode!(conn.resp_body), &(&1["cell"] == "z9z9z9"))
  end

  test "mailbox deposit then collect round-trips opaque bytes, then drains" do
    env = Base.encode64(<<1, 2, 3, 4, 5>>)

    assert call(conn(:post, "/mailbox/box123", Jason.encode!(%{envelope: env}))
                |> put_req_header("content-type", "application/json")).status == 204

    conn = call(conn(:get, "/mailbox/box123"))
    assert conn.status == 200
    assert Jason.decode!(conn.resp_body) == %{"envelopes" => [env]}

    # drained
    conn2 = call(conn(:get, "/mailbox/box123"))
    assert Jason.decode!(conn2.resp_body) == %{"envelopes" => []}
  end
end
