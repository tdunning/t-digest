pdf(file="qd-sizes.pdf", width=4.5, height=2, pointsize=9)
require(dplyr)

layout(matrix(c(1,2), nrow=1))

par(cex.axis=0.8)
par(cex.lab=1.1)
par(mar=c(3.2, 3.3, 0.2, 0.2))
par(mgp=c(2.0, 0.5, 0))
par(tcl=-0.3)
par(las=2)

comp = read.csv("qd-tree-comparison.csv")
filtered = comp %>% filter(tag == "uniform")
plot(q.size ~ compression, filtered, pch=21, col=NA, bg=rgb(0,0,0,alpha=0.01), log='xy', yaxt='n', xlab=expression(delta), ylab="Digest Size (bytes)", ylim=c(70,6e3), cex=0.4, xlim=c(7,1100))
points(t.size ~ compression, filtered, pch=21, col=NA, bg=rgb(0,0,0,alpha=0.01), cex=0.4)
lines(q.size ~ compression, filtered %>% group_by(compression) %>% summarise(q.size=mean(q.size)), type='c')
lines(t.size ~ compression, filtered %>% group_by(compression) %>% summarise(t.size=mean(t.size)), type='c')

axis(side=2, at=c(100, 200, 500, 1000,2e3,5e3), labels=c("100", "200", "500", "1kB","2kB","5kB"))
lines(c(7,16), c(954,954), col='grey')
lines(c(25,160), c(954,954), col='grey')
lines(c(250,1100), c(954,954), col='grey')

text(25, 2500, expression(Q-digest), cex=0.7)
text(400, 4000, expression(italic(t)-digest), cex=0.7)

lines(c(20, 20), c(750, 270), col='grey')
lines(c(20, 20), c(180, 110), col='grey')
text(20, 85, expression(delta[italic(q)]==20), cex=0.7)

lines(c(200, 200), c(750, 110), col='grey')
text(200, 85, expression(delta[italic(t)]==200), cex=0.7)

boxplot(e2 ~ q, comp %>% filter(tag == "uniform", compression==20 ), boxwex=0.3, at=1:11-0.17, xaxt='n', col='grey', ylab="Absolute Error", xlab=expression(italic(q)), ylim=c(0,0.091), lwd=0.15, cex=0.4)
boxplot(e1 ~ q, comp %>% filter(tag == "uniform", compression==200 ), boxwex=0.3, at=1:11+0.17, xaxt='n', add=T, lwd=0.15, cex=0.4)
axis(side=1, at=1:11, labels=c("0.00001", "0.0001", "0.001", "0.01", "0.1", "0.5", "0.9", "0.99", "0.999", "0.9999", "0.99999"), las=2)

abline(h=0, col=rgb(0,0,0,alpha=0.2), lwd=1)
legend(4.9, 0.092, legend=expression(italic(t)-"digest  " (delta[italic(t)]==200), Q-"digest "(delta[italic(q)]==20)), fill=c('white', 'grey'), cex=0.65, bg='white')


dev.off()
