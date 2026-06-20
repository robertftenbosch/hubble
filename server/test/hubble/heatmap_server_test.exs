defmodule Hubble.Heatmap.ServerTest do
  use ExUnit.Case, async: true

  alias Hubble.Heatmap.Server

  setup do
    # Inject a fixed clock so the test is deterministic and TTL-independent.
    {:ok, pid} = Server.start_link(name: nil, now_fun: fn :millisecond -> 0 end)
    %{server: pid}
  end

  test "records beacons and reports a cell once it meets the k floor", %{server: s} do
    assert Server.heatmap(s) == []
    for _ <- 1..3, do: Server.record(s, "u15hy7aa")
    assert Server.heatmap(s) == [%{cell: "u15hy7", count: 3}]
  end

  test "a single beacon stays suppressed (privacy floor)", %{server: s} do
    Server.record(s, "u15hy7aa")
    assert Server.heatmap(s) == []
  end
end
