errorData = read.delim("errors.csv")

plotError = function(dist, ylim=c(-2000, 2000), yaxt='s', ylab, text) {
  boxplot(1e6*error ~ Q, errorData[errorData$dist==dist,], ylim=ylim,
          xlab="Quantile (q)", ylab=ylab, yaxt=yaxt, boxwex=0.4,
          main = text)
  box()
  abline(h=1000, lty=2)
  abline(h=-1000, lty=2)
}

eldest = par(lwd=0.2)
pdf("error.pdf", width=6, height=3, pointsize=7)
layout(matrix(c(1,2), 1, 2, byrow=T), heights=c(1200, 1200), widths=c(1285,1100))
#plotError('mixture', 'mixture-error.png')
plotError('uniform', ylab="Quantile error (ppm)", text=expression(Uniform))
old = par(mar=c(5.1,0,4.1,2))
plotError('gamma', yaxt='n', ylab=NA, text=expression(Gamma(0.1, 0.1)))
par(old)
  dev.off()
par(eldest)
