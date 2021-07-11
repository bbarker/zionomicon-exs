with import <nixpkgs> {};
stdenv.mkDerivation {
  name = "BE_Bonus_Service_Environment";
  buildInputs = [
    jdk16_headless
    metals
    sbt
  ];
  src = null;
  shellHook = ''
    echo "javac's location is $(type javac)"
  '';
}

# Note, can optionally launch idea from a nix-shell enviornment.
# 1. Run `nix-shell` from this directory
# 2. Then run `nohup /usr/local/bin/idea &`
# Unfortunately this does not propagate to idea's terminals