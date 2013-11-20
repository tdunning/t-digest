errorData = read.delim("errors.csv")

plotError = function(title, dist, ylim=c(-2000, 2000), yaxt='s', ylab) {
  boxplot(1e6*error ~ Q, errorData[errorData$dist==dist,], ylim=ylim, lwd=4, xlab="Cumulative Distribution", ylab=ylab, pars=list(lwd.ticks=4), yaxt=yaxt, main=title)
  box(lwd=8)
  abline(h=1000, lty=2, lwd=4)
  abline(h=-1000, lty=2, lwd=4)
}

  png("error.png", width=2400, height=1200, pointsize=36)
layout(matrix(c(1,2), 1, 2, byrow=T), heights=c(1200, 1200), widths=c(1300,1100))
#plotError('mixture', 'mixture-error.png')
plotError(expression(paste("Uniform Distribution")), 'uniform', ylab="Error (ppm)")
old = par(mar=c(5.1,0,4.1,2))
plotError(expression(paste(Gamma(0.1,0.1)," Distribution")), 'gamma', yaxt='n', ylab=NA)
par(old)
  dev.off()
  png("sequential-error.png", width=2400, height=1200, pointsize=36)
plotError('Sequential Values', 'sequential', yaxt='n', ylab=NA)
  dev.off()
