#/bin/bash
# git config core.hooksPath $PWD/config/hooks
# safe fall-back for older git versions
ln -s ./config/hooks/pre* ./.git/hooks/
