mark = function(x) {arrows(x,9, x,5, length=0.15, angle=10, lwd=2)}

z = rnorm(2000)
shift = c(rep(0, 400), rep(1.5, 600), rep(-2, 300), rep(-1, 700))
spike = c(rep(0,1600), rep(9, 5), rep(395,0))
plot(z+shift+spike, type='s', xlab='Time', ylab='Shift + Noise', ylim=c(-10,10))
mark(400)
mark(1000)
mark(1300)


