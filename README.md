Adds A Flinger Block to hytale to make pipeless logistics fun!

for issues for modjam: 
-if null block state, the flinger needs an output either 4 blocks away or 3
-some automation mods cause the tick references items removed which can cause out of sync item animation
-the container only takes in 1 item at a time, this INCLUDES itemstacks, if your itemstack is any higher than one, the inventory will reject the item as the animation is based on a single state tick timer, referencing multiple in the time alotted was out my experience
