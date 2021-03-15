sudo sshfs -o allow_other,defer_permissions,IdentityFile=~/.ssh/id_rsa pi@axidraw:/home/pi/grootlapse /Users/thomas/projects/grootlapse/out;
cd src/grootlapse/;
ls | entr -s 'shadow-cljs release raspi';



