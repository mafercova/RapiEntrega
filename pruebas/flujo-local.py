"""Recorrido HTTP de ocho procesos Java con H2 temporal, después de mvn verify.
No necesita MySQL, no modifica bases existentes y detiene solo los procesos creados.
Requiere JDK 21 en JAVA_HOME y Python 3.12+.
"""
from pathlib import Path
import subprocess,os,time,socket,xml.etree.ElementTree as ET,json,importlib.util,traceback
r=Path(__file__).resolve().parents[1];work=r/'target/flujo-local';work.mkdir(parents=True,exist_ok=True)
mods=['catalogo','pedidos','pagos','logistica','facturacion','calificaciones'];procs=[];logs=[];ports={}
for port in [8081,8082,8083,8084,8085,8086,8091,8092]:
 with socket.socket() as s:s.bind(('127.0.0.1',0));ports[port]=s.getsockname()[1]
try:
 for i,name in enumerate(mods+['pasarela-mock','facturacion-legado-mock']):
  base=r/('servicios/'+name+'-service' if i<6 else 'integraciones/'+name)
  report=next(base.glob('target/surefire-reports/TEST-*.xml'));props={x.attrib['name']:x.attrib.get('value','') for x in ET.parse(report).getroot().findall('properties/property')};cp=props['surefire.test.class.path'];pkg=name if i<6 else ('pasarela'if i==6 else'legado');env=os.environ.copy();env.pop('JAVA_TOOL_OPTIONS',None)
  env['PORT']=str(ports[8081+i if i<6 else 8091+i-6]);env['SPRING_PROFILES_ACTIVE']='demo'
  if i<6:env.update(DB_URL=f'jdbc:h2:mem:e2e_{name};MODE=MySQL;DB_CLOSE_DELAY=-1',DB_USER='sa',DB_PASSWORD='')
  for j,n in enumerate(mods):env[n.upper()+'_URL']='http://localhost:'+str(ports[8081+j])
  env['PASARELA_URL']='http://localhost:'+str(ports[8091]);env['LEGADO_URL']='http://localhost:'+str(ports[8092]);log=open(work/(name+'.log'),'w',encoding='utf-8');logs.append(log)
  cmd=[str(Path(os.environ['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java')),'-Xms32m','-Xmx192m','-Dlogging.level.root=WARN','-cp',cp,'mx.edu.rapientrega.'+pkg+'.Application']
  procs.append(subprocess.Popen(cmd,cwd=base,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0)))
 print('Ocho aplicaciones de prueba iniciadas en puertos temporales.',flush=True)
 deadline=time.monotonic()+180
 while time.monotonic()<deadline:
  if any(p.poll() is not None for p in procs):raise RuntimeError('Una aplicación terminó; revisar logs de target/flujo-local')
  good=True
  for port in ports.values():
   try:
    with socket.create_connection(('127.0.0.1',port),timeout=.3):pass
   except OSError:good=False
  if good:break
  time.sleep(1)
 else:raise RuntimeError('Tiempo de inicio agotado')
 print('Aplicaciones listas; ejecutando flujo HTTP completo.',flush=True)
 spec=importlib.util.spec_from_file_location('smoke',r/'pruebas/smoke.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);origcall=mod.call;origready=mod.ready
 mod.call=lambda port,*a,**kw:origcall(ports[port],*a,**kw)
 mod.ready=lambda port:origready(ports[port])
 mod.main()
 (work/'resultado.json').write_text(json.dumps({'resultado':'APROBADO','persistencia':'H2 temporal por servicio','transporte':'HTTP real en ocho procesos Spring Boot','casos':['entrega y ubicación','calificación','factura','timeout conciliado','rechazo y reembolso','pago rechazado']},ensure_ascii=False,indent=2),encoding='utf-8')
 print('E2E nativo aprobado con H2; no equivale a verificación de MySQL.',flush=True)
finally:
 for p in procs:
  if p.poll() is None:p.terminate()
 for p in procs:
  try:p.wait(timeout=10)
  except subprocess.TimeoutExpired:p.kill()
 for log in logs:log.close()
