import Config

# HTTP port: ephemeral (0) under test so the suite never collides with a running
# dev server; 4000 (or $PORT) otherwise.
port =
  if config_env() == :test do
    0
  else
    String.to_integer(System.get_env("PORT") || "4000")
  end

config :hubble, :port, port
