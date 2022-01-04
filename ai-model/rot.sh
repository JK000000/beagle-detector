i=1

for szFile in ./new/imgn/*.jpg
do 
    convert "$szFile" -rotate 90 ./rot/"$(basename "$szFile")" ; 
    echo $i

    i=$((i+1))
done

