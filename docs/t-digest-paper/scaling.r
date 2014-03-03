data = read.delim("scaling.tsv")
errors = read.delim("error-scaling.tsv")
errors$kb = errors$size/1000

pdf("scaling.pdf", width=6, height=2.5, pointsize=7)
layout(matrix(c(1,2), 1, 2, byrow=T), widths=c(1,1))

old = par(mar=c(5.1,4,2.1,2), lwd=0.2)

plot(size1 ~ compression, data[data$samples==10000,], log='xy',
     xlab=expression(1/delta), ylab="Size (kB)", ylim=c(100,100000),
     cex=1, bg=rgb(0,0,0,0.1), col=rgb(0,0,0,0.1), pch=21,
     yaxt='n')
axis(at=c(100,1000,10000,100000), labels=c("0.1", "1", "10", "100"), side=2)
box()

#points(size1 ~ compression, data[data$samples==100,], log='xy',
#     xlab=expression(1/delta), ylab="Size (kB)", 
#       cex=0.5, bg=rgb(0,0,0,0.1), col=rgb(0,0,0,0.1), pch=21)
#
points(size1 ~ compression, data[data$samples==10,], 
       cex=1, bg=rgb(0,0,0,0.1), col=rgb(0,0,0,0.1), pch=22)

legend(2, 9e4, c("10M samples", "10k samples"), pch=c(21,22)) 

c = seq(2,1100,by=1)
m10 = lm(log(size1) ~ log(compression), data[data$samples==10,])
m100 = lm(log(size1) ~ log(compression), data[data$samples==100,])
m10000 = lm(log(size1) ~ log(compression), data[data$samples==10000,])
lines(c, exp(predict(m10, newdata=data.frame(compression=c))), lty=2, col='lightgray')
#lines(c, exp(predict(m100, newdata=data.frame(compression=c))), lty=2, col='lightgray')
lines(c, exp(predict(m10000, newdata=data.frame(compression=c))), lty=2, col='lightgray')
par(old)

old = par(mar=c(5.1,4.2,2.1,2), lwd=0.2)
plot(size2/1000 ~ samples, data[data$compression==100,], log='x', ylim=c(0,22),
     xlab="Samples (x1000)", ylab="Size (kB)", xaxt='n',
     cex=1, bg=rgb(0,0,0,0.1), col=rgb(0,0,0,0.1), pch=21, main=expression(1/delta == 100))
axis(at=c(10,100,1000,10000), labels=c(10,100,1000,"10,000"), side=1)
points(size1/1000 ~ samples, data[data$compression==100,],
       cex=1, bg=rgb(0,0,0,0.1), col=rgb(0,0,0,0.1), pch=22)
ms2 = lm(size2/1000 ~ log(samples), data[data$compression==100,])
s = seq(10, 10000, by=10)
lines(s, predict(ms2, newdata=data.frame(samples=s)), lty=2, col='lightgray')

ms1 = lm(size1/1000 ~ log(samples), data[data$compression==100,])
lines(s, predict(ms1, newdata=data.frame(samples=s)), lty=2, col='lightgray')

samples = c(10,100,1000,10000)
mpSize = 6*((samples*1000) ^ 0.5 * 4)/1000
lines(samples, mpSize, lty=5, pch=23, type='b', lwd=0.8, col='grey')
points(samples, mpSize, lty=5, pch=23)

#box()

legend(9, 22, c("uncompressed", "compressed", "Munro-Paterson"), pch=c(21,22,23)) 

par(old)

dev.off()

pdf("error-scaling.pdf", width=6, height=2, pointsize=7)
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1.18,1,1))

for (q in c(0.5, 0.01, 0.001)) {
  if (q == 0.5) {
    old = par(mar=c(5.1,4.5,2.1,2))
  } else {
    old = par(mar=c(5.1,0,2.1,2))
  }
  par(lwd=0.2)
  top = paste("q =", q)
  plot(error ~ kb, errors[errors$q == q,],
       ylim=c(-0.05, 0.05),
       pch=21, bg=rgb(0,0,0,.1), col=rgb(0,0,0,.1), log='x',
       xlab="t-digest size (kB)", ylab="Quantile error", cex.lab=1.5,
       yaxt='n', main=top)
  abline(h=0, col='lightgray')
  abline(h=0.01, col='lightgray', lty=2)
  abline(h=-0.01, col='lightgray', lty=2)

  box()
  if (q == 0.5) {
    axis(side=2)
  }
  text(20, 0.09, paste("q =",q))
  par(old)
}

dev.off()
