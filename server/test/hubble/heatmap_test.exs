defmodule Hubble.HeatmapTest do
  use ExUnit.Case, async: true

  defp beacon(geohash, at_ms), do: %{geohash: geohash, at_ms: at_ms}

  test "expires beacons older than the TTL window" do
    now = 1_000_000
    beacons = [beacon("u15hy7", now - 100), beacon("u15hy7", now - 2_000_000)]
    assert Hubble.Heatmap.expire(beacons, now, ttl_ms: 900_000) == [beacon("u15hy7", now - 100)]
  end

  test "k-anonymity: a cell below k beacons is suppressed" do
    now = 0
    beacons = [beacon("u15hy7", 0), beacon("u15hy7", 0)] # only 2, k defaults to 3
    assert Hubble.Heatmap.cells(beacons, now) == []
  end

  test "k-anonymity: a cell at or above k is reported with its count" do
    now = 0
    beacons = for _ <- 1..3, do: beacon("u15hy7", 0)
    assert Hubble.Heatmap.cells(beacons, now) == [%{cell: "u15hy7", count: 3}]
  end

  test "buckets distinct fine geohashes into the same coarse cell" do
    now = 0
    beacons = [beacon("u15hy7aa", 0), beacon("u15hy7bb", 0), beacon("u15hy7cc", 0)]
    assert Hubble.Heatmap.cells(beacons, now, precision: 6) == [%{cell: "u15hy7", count: 3}]
  end

  test "expired beacons do not count toward the k floor" do
    now = 1_000_000
    beacons = [
      beacon("u15hy7", now),
      beacon("u15hy7", now),
      beacon("u15hy7", now - 2_000_000) # stale -> drops the cell below k=3
    ]
    assert Hubble.Heatmap.cells(beacons, now) == []
  end

  test "cells are sorted by descending count" do
    now = 0
    beacons =
      (for _ <- 1..3, do: beacon("aaaaaa", 0)) ++
        (for _ <- 1..5, do: beacon("bbbbbb", 0))

    assert Hubble.Heatmap.cells(beacons, now) == [
             %{cell: "bbbbbb", count: 5},
             %{cell: "aaaaaa", count: 3}
           ]
  end
end
