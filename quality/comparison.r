pdf(file="qd-sizes.pdf", width=4, height=2, pointsize=9)
require(dplyr)

layout(matrix(c(1,2), nrow=1))

par(cex.axis=0.8)
par(cex.lab=1.3)
par(mar=c(3.9, 4.1, 0.2, 0.2))
par(mgp=c(2.5, 0.5, 0))
par(tcl=-0.3)
par(las=2)

comp = read.csv("qd-tree-comparison.csv")
filtered = comp %>% filter(tag == "uniform")
plot(q.size ~ compression, filtered, pch=21, col=NA, bg=rgb(0,0,0,alpha=0.01), log='xy', yaxt='n', xlab=expression(delta), ylab="Digest Size (bytes)", ylim=c(100,120e3), cex=0.4)
points(t.size ~ compression, filtered, pch=21, col=NA, bg=rgb(0,0,0,alpha=0.01), cex=0.4)
lines(q.size ~ compression, filtered %>% group_by(compression) %>% summarise(q.size=mean(q.size)), type='c')
lines(t.size ~ compression, filtered %>% group_by(compression) %>% summarise(t.size=mean(t.size)), type='c')

axis(side=2, at=c(100, 1000,10e3,100e3), labels=c("100", "1kB","10e3","100k"))
lines(c(5,40), c(2343.2,2343.2), col='grey')
lines(c(60,400), c(2343.2,2343.2), col='grey')
lines(c(600,2000), c(2343.2,2343.2), col='grey')

text(230, 29000, "Q-digest", cex=0.7)
text(750, 8000, "t-digest", cex=0.7)

lines(c(50, 50), c(1800, 450), col='grey')
lines(c(50, 50), c(270, 200), col='grey')
text(50, 130, expression(delta[q]==50), cex=0.7)

lines(c(500, 500), c(1800, 200), col='grey')
text(500, 130, expression(delta[t]==500), cex=0.7)

boxplot(e2 ~ q, comp %>% filter(tag == "uniform", compression==50 ), boxwex=0.3, at=1:11-0.17, xaxt='n', col='grey', ylab="Absolute Error", xlab=expression(q), ylim=c(0,0.021), lwd=0.15, cex=0.4)
boxplot(e1 ~ q, comp %>% filter(tag == "uniform", compression==500 ), boxwex=0.3, at=1:11+0.17, xaxt='n', add=T, lwd=0.15, cex=0.4)
axis(side=1, at=1:11, labels=c("0.00001", "0.0001", "0.001", "0.01", "0.1", "0.5", "0.9", "0.99", "0.999", "0.9999", "0.99999"), las=2)

legend(3.3, 0.0205, legend=expression("t-digest "(delta[t]==500), "Q-digest "(delta[q]==50)), fill=c('white', 'grey'), cex=0.65)


dev.off()
