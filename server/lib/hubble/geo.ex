defmodule Hubble.Geo do
  @moduledoc """
  Standard [geohash](https://en.wikipedia.org/wiki/Geohash) encoding (base32) and
  prefix bucketing. Geohashes are hierarchical: truncating a hash to a shorter prefix
  yields a coarser cell, which is exactly how the heatmap limits precision for privacy.

  Approximate cell sizes:
    * length 5 ≈ 4.9 km × 4.9 km
    * length 6 ≈ 1.2 km × 0.6 km
    * length 7 ≈ 153 m × 153 m
  """

  @base32 ~c"0123456789bcdefghjkmnpqrstuvwxyz"

  @doc "Encode a lat/lon to a geohash of the given length (default 6)."
  @spec encode(float(), float(), pos_integer()) :: String.t()
  def encode(lat, lon, precision \\ 6)
      when is_number(lat) and is_number(lon) and precision > 0 do
    bits = encode_bits(lat, lon, {-90.0, 90.0}, {-180.0, 180.0}, true, [], precision * 5)
    bits
    |> Enum.chunk_every(5)
    |> Enum.map(&Enum.reduce(&1, 0, fn b, acc -> acc * 2 + b end))
    |> Enum.map(&Enum.at(@base32, &1))
    |> List.to_string()
  end

  @doc "Truncate a geohash to a coarser precision (its prefix). Never extends it."
  @spec coarsen(String.t(), pos_integer()) :: String.t()
  def coarsen(geohash, precision) when is_binary(geohash) and precision > 0 do
    String.slice(geohash, 0, precision)
  end

  # Interleave longitude (even bit positions) and latitude (odd) bits, refining the
  # range by binary search until we've produced the requested number of bits.
  defp encode_bits(_lat, _lon, _lat_r, _lon_r, _even?, acc, 0), do: Enum.reverse(acc)

  defp encode_bits(lat, lon, {lat_lo, lat_hi}, {lon_lo, lon_hi}, even?, acc, n) do
    if even? do
      mid = (lon_lo + lon_hi) / 2

      if lon >= mid,
        do: encode_bits(lat, lon, {lat_lo, lat_hi}, {mid, lon_hi}, false, [1 | acc], n - 1),
        else: encode_bits(lat, lon, {lat_lo, lat_hi}, {lon_lo, mid}, false, [0 | acc], n - 1)
    else
      mid = (lat_lo + lat_hi) / 2

      if lat >= mid,
        do: encode_bits(lat, lon, {mid, lat_hi}, {lon_lo, lon_hi}, true, [1 | acc], n - 1),
        else: encode_bits(lat, lon, {lat_lo, mid}, {lon_lo, lon_hi}, true, [0 | acc], n - 1)
    end
  end
end
