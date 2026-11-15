import base64,sys
src=open('src.html').read()
css=''
for w,n in [(400,'Regular'),(500,'Medium'),(600,'SemiBold'),(700,'Bold')]:
    b=base64.b64encode(open(f'fonts/Inter-{n}.woff','rb').read()).decode()
    css+=f"@font-face{{font-family:Inter;font-weight:{w};font-style:normal;font-display:swap;src:url(data:font/woff;base64,{b}) format('woff')}}\n"
open(sys.argv[1],'w').write(src.replace('/*FONTS*/',css))
