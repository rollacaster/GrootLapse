sshfs pi@axidraw:/home/pi/grootlapse /Users/thomas/projects/grootlapse/out;
cd src/grootlapse/;
ls server.cljs | entr -s 'shadow-cljs release raspi';



