{ pkgs, ... }:

{
  env.GREET = "http-end2end-encryption";

  packages = [ pkgs.gitAndTools.gitFull ];

  languages.kotlin.enable = true;
  languages.java.enable = true;
  languages.java.maven.enable = true;

  pre-commit.hooks.prettier.enable = true;

  enterTest = ''
    mvn test
  '';
}
