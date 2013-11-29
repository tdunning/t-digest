data = read.delim("comparison.tsv")
keep = function(data, tag) {
  data[data$dist == tag & data$compression == 50 & data$q !=0.3 & data$q != 0.7 & data$q != 0.2 & data$q != 0.8, ]
}

png("qd-sizes.png", width=1800, height=700, pointsize=28)
layout(matrix(c(1,2,3), 1, 3, byrow=T), widths=c(1,1))

gray = rgb(0,0,0,0.05)

old = par(mar=c(4.5,5,3,0.5))
plot(s2~s1, data, log='xy', pch=21, col=gray, bg=gray, cex=0.4,
     xlab="t-digest (bytes)", ylab="Q-digest (bytes)",
     xlim=c(100, 120000),
     cex.lab=1.5, xaxt='n', yaxt='n')
box(lwd=3)

axis(at=c(100, 300, 1000, 3000, 10000, 30000, 100000), labels=c(100, 300, "1K", "3K", "10K", "30K", "100K"), side=1)
axis(at=c(100, 300, 1000, 3000, 10000, 30000, 100000), labels=c(100, 300, "1K", "3K", "10K", "30K", "100K"), side=2)

steps = exp(seq(log(100), log(200000), by=log(2)))
lines(steps, steps, col='lightgrey')
lines(steps, steps/2, lty=2, col='lightgrey')
lines(steps,steps*2, lty=2, col='lightgrey')

for (compression in c(2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000)) {
  x = mean(data[data$compression == compression,]$s1) * 1.8
  y = mean(data[data$compression == compression,]$s2)
  text(x,y,compression)
}
text(10000, 1000, expression(1/delta), cex=1.5)

par(old)

old = par(mar=c(4.5,5,3,0.5))
boxplot(1e6*e2 ~ q, keep(data, 'uniform'), at=1:7 - 0.13, boxwex=0.3, xaxt='n', yaxt='n',
        ylab="Quantile error (ppm)", xlab="Quantile",
        ylim=c(-10000, 20000), cex.lab=1.5, col=rgb(0.95, 0.95, 0.95))
boxplot(1e6*e1 ~ q, keep(data, 'uniform'), col=rgb(0.4, 0.4, 0.4), at=1:7 + 0.13, add=T, boxwex=0.3, xaxt='n', yaxt='n')
axis(at=1:7, labels=c(0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999), side=1)
axis(side=2, cex.label=2)
abline(h=0, lwd=2, col='gray')
for (i in 1:7) {
  abline(v=i, lwd=1, col='lightgray', lty=2)
}
legend(5.5, 20000, c("Q-digest", "t-digest"), fill = c(rgb(0.95, 0.95, 0.95), rgb(0.4, 0.4, 0.4)))
text(6.5, 14000, "Uniform", cex=1.5)
text(6.5, 12000, expression(1/delta == 50), cex=1.5)
box(lwd=3)
par(old)

old = par(mar=c(4.5,5,3,0.5))
boxplot(1e6*e2 ~ q, keep(data, 'gamma'), col=rgb(0.95, 0.95, 0.95), at=1:7 - 0.13, boxwex=0.3, xaxt='n',
        ylab="Quantile error (ppm)", xlab="Quantile",
        cex.lab=1.5)
boxplot(1e6*e1 ~ q, keep(data, 'gamma'), col=rgb(0.4, 0.4, 0.4), at=1:7 + 0.13, add=T, boxwex=0.3, xaxt='n')
axis(at=1:7, labels=c(0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999), side=1)
abline(h=0, lwd=2, col='gray')
for (i in 1:7) {
  abline(v=i, lwd=1, col='lightgray', lty=2)
}
legend(5.5, 88000, c("Q-digest", "t-digest"), fill = c(rgb(0.95, 0.95, 0.95), rgb(0.4, 0.4, 0.4)))
text(6.5, 68000, expression(Gamma(0.1, 0.1)), cex=1.5)
text(6.5, 62000, expression(1/delta == 50), cex=1.5)
box(lwd=3)
par(old)

dev.off()
