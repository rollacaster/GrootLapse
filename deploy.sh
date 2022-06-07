shadow-cljs release raspi; scp out/grootlapse-pi.js pi@axidraw:/home/pi/grootlapse/grootlapse-pi.js
shadow-cljs release app; scp out/public/js/main.js pi@axidraw:/home/pi/grootlapse/public/js

scp package.json pi@axidraw:/home/pi/grootlapse/package.json
scp out/public/index.html pi@axidraw:/home/pi/grootlapse/public/index.html
scp out/public/default.png pi@axidraw:/home/pi/grootlapse/public/default.png
scp out/public/imgs/groot-sad.gif pi@axidraw:/home/pi/grootlapse/public/imgs/groot-sad.gif
scp out/public/vendor/http-live-player-worker.js pi@axidraw:/home/pi/grootlapse/public/vendor/http-live-player-worker.js
scp out/public/vendor/http-live-player.js pi@axidraw:/home/pi/grootlapse/public/vendor/http-live-player.js

scp out/public/js/manifest.edn pi@axidraw:/home/pi/grootlapse/public/js
