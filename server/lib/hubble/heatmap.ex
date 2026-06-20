defmodule Hubble.Heatmap do
  @moduledoc """
  Pure, privacy-preserving heatmap aggregation. Given a list of anonymous beacons and
  the current time, it expires stale beacons, buckets them into coarse geohash cells,
  and **suppresses any cell with fewer than `k` beacons** so individuals cannot be
  located (k-anonymity). No identity is ever involved — a beacon is `{geohash, at_ms}`.

  These functions are deterministic and side-effect free; the GenServer that holds live
  beacons (`Hubble.Heatmap.Server`) delegates here. Defaults encode the privacy policy:

    * `:precision`  6  — reported cell size (≈ 1.2 km); also the max ingest precision
    * `:k`          3  — minimum beacons per reported cell
    * `:ttl_ms`     900_000 (15 min) — freshness window
  """

  @type beacon :: %{geohash: String.t(), at_ms: integer()}
  @type cell :: %{cell: String.t(), count: non_neg_integer()}

  @default_precision 6
  @default_k 3
  @default_ttl_ms 900_000

  @doc "Drop beacons older than the TTL relative to `now_ms`."
  @spec expire([beacon], integer(), keyword()) :: [beacon]
  def expire(beacons, now_ms, opts \\ []) do
    ttl = Keyword.get(opts, :ttl_ms, @default_ttl_ms)
    Enum.filter(beacons, fn b -> now_ms - b.at_ms < ttl end)
  end

  @doc """
  Build the heatmap: expire stale beacons, bucket by coarse geohash prefix, count per
  cell, and withhold cells below the k-anonymity floor. Returns cells sorted by
  descending count (ties broken by cell name for determinism).
  """
  @spec cells([beacon], integer(), keyword()) :: [cell]
  def cells(beacons, now_ms, opts \\ []) do
    precision = Keyword.get(opts, :precision, @default_precision)
    k = Keyword.get(opts, :k, @default_k)

    beacons
    |> expire(now_ms, opts)
    |> Enum.group_by(fn b -> Hubble.Geo.coarsen(b.geohash, precision) end)
    |> Enum.map(fn {cell, list} -> %{cell: cell, count: length(list)} end)
    |> Enum.filter(fn %{count: c} -> c >= k end)
    |> Enum.sort_by(fn %{cell: cell, count: c} -> {-c, cell} end)
  end

  @doc "Normalize an inbound beacon to the allowed (coarse) ingest precision."
  @spec normalize_geohash(String.t(), keyword()) :: String.t()
  def normalize_geohash(geohash, opts \\ []) do
    precision = Keyword.get(opts, :precision, @default_precision)
    Hubble.Geo.coarsen(geohash, precision)
  end
end
