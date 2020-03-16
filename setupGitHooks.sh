#/bin/bash
# git config core.hooksPath $PWD/config/hooks
# safe fall-back for older git versions
ln -s -r ./config/hooks/pre* -t ./.git/hooks/
