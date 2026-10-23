"""Prueba HTTP de extremo a extremo sobre las ocho aplicaciones locales. Python 3, sin dependencias."""
import json, time, uuid, urllib.request, urllib.error

def call(port,path,body=None,key=None):
 data=None if body is None else json.dumps(body).encode()
 req=urllib.request.Request(f'http://localhost:{port}'+path,data=data,headers={'Content-Type':'application/json','Idempotency-Key':key or str(uuid.uuid4())})
 with urllib.request.urlopen(req,timeout=6) as response:return json.load(response)

def wait(fn,predicate=lambda x:True,timeout=120):
 deadline=time.monotonic()+timeout;last=None
 while time.monotonic()<deadline:
  try:
   last=fn()
   if predicate(last):return last
  except (urllib.error.URLError,TimeoutError) as e:last=str(e)
  time.sleep(1)
 raise AssertionError(f'No se alcanzó condición; último resultado={last}')

def order():
 return call(8082,'/api/v1/pedidos',{'clienteId':'cliente-smoke','restauranteId':1,'items':[{'productoId':1,'cantidad':2}],'entrega':{'latitud':19.44,'longitud':-99.14,'direccion':'Campus'}})

def paid(p,token='ok'):
 result=call(8083,'/api/v1/pagos',{'pedidoId':p['id'],'tokenTarjeta':token})
 return result

def ready(port):
 try:
  with urllib.request.urlopen(f'http://localhost:{port}/',timeout=3):return True
 except urllib.error.HTTPError as e:
  return e.code in (200,404)

def main():
 for port in (8081,8082,8083,8084,8085,8086,8091,8092):wait(lambda port=port:ready(port),timeout=240)
 wait(lambda:call(8081,'/api/v1/restaurantes'))
 p=order();assert p['total']==131
 paid(p)
 wait(lambda:call(8082,'/api/v1/pedidos/'+p['id']),lambda x:x['estado']=='PENDIENTE_ACEPTACION')
 call(8082,'/api/v1/pedidos/'+p['id']+'/aceptacion',{})
 a=wait(lambda:call(8084,'/api/v1/asignaciones/'+p['id']),lambda x:x['estado']=='ASIGNADO')
 stamp=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime())
 call(8084,'/api/v1/asignaciones/'+p['id']+'/ubicaciones',{'repartidorId':a['repartidorId'],'latitud':19.44,'longitud':-99.14,'capturedAt':stamp,'sequence':1})
 call(8084,'/api/v1/asignaciones/'+p['id']+'/entrega',{'repartidorId':a['repartidorId']})
 wait(lambda:call(8082,'/api/v1/pedidos/'+p['id']),lambda x:x['estado']=='ENTREGADO')
 c=wait(lambda:call(8086,'/api/v1/calificaciones',{'pedidoId':p['id'],'clienteId':'cliente-smoke','puntuacion':5,'comentario':'Prueba E2E'}));assert c['puntuacion']==5
 wait(lambda:call(8085,'/api/v1/facturas/'+p['id']),lambda x:x['estado']=='EMITIDA')
 p2=order();paid(p2,'timeout-once')
 wait(lambda:call(8082,'/api/v1/pedidos/'+p2['id']),lambda x:x['estado']=='PENDIENTE_ACEPTACION')
 call(8082,'/api/v1/pedidos/'+p2['id']+'/rechazo',{})
 wait(lambda:call(8083,'/api/v1/pagos/pedido/'+p2['id']),lambda x:x['estado']=='REEMBOLSADO')
 p3=order();paid(p3,'reject')
 wait(lambda:call(8082,'/api/v1/pedidos/'+p3['id']),lambda x:x['estado']=='PAGO_RECHAZADO')
 print('OK E2E: entrega, ubicación, calificación, factura, timeout conciliado, rechazo/reembolso y tarjeta rechazada')
if __name__=='__main__':main()
