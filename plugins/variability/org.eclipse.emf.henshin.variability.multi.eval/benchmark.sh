#!/bin/bash
start=`date +%s`
for number in {0..5}
do
java -Dnashorn.args=--no-deprecation-warning -jar benchmark.jar vb "$number" --runs 10
java -Dnashorn.args=--no-deprecation-warning -jar benchmark.jar lifted "$number" --runs 10
done
end=`date +%s`
echo Execution took `expr $end - $start` seconds
java -cp bin/ org.eclipse.emf.henshin.variability.multi.eval.DataPreparation
exit 0
