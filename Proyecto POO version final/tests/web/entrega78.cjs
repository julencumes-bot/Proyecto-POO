/* Prueba real de navegador. Datos temporales, cuentas ficticias, escritorio y celular. */
const {chromium}=require('playwright');
const {spawn}=require('node:child_process');
const fs=require('node:fs/promises');
const path=require('node:path');
const os=require('node:os');
const assert=require('node:assert/strict');
let checks=0;
function check(value,message){assert.ok(value,message);checks++;}
const day=n=>{const d=new Date();d.setUTCDate(d.getUTCDate()+n);return d.toISOString().slice(0,10);};
async function main(){
 const tmp=await fs.mkdtemp(path.join(os.tmpdir(),'tutorias-78-'));
 const output=process.env.TEST_OUTPUT||path.join(tmp,'capturas');await fs.mkdir(output,{recursive:true});
 const server=spawn('java',['-cp',process.platform==='win32'?'bin;lib/*':'bin:lib/*','WebMain'],{env:{...process.env,PORT:'0',DEMO_DATA:'true',DB_URL:'jdbc:h2:'+path.join(tmp,'datos')+';MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE'},stdio:['ignore','pipe','pipe']});
 let browser;const errors=[];
 try{
  const url=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('Servidor no inició')),20000);server.stdout.on('data',d=>{const m=d.toString().match(/http:\/\/127\.0\.0\.1:\d+/);if(m){clearTimeout(timer);resolve(m[0]);}});server.on('exit',code=>{clearTimeout(timer);reject(Error('Servidor: '+code));});server.stderr.on('data',d=>process.stderr.write(d));});
  browser=await chromium.launch({headless:true,...(process.env.BROWSER_EXECUTABLE?{executablePath:process.env.BROWSER_EXECUTABLE}:process.env.BROWSER_CHANNEL?{channel:process.env.BROWSER_CHANNEL}:{})});
  async function login(rol,carnet){const ctx=await browser.newContext({viewport:{width:1440,height:1000}});const p=await ctx.newPage();p.on('pageerror',e=>errors.push(e.message));await p.goto(url);await p.locator('#rol').selectOption(rol);await p.locator('#carnet').fill(carnet);await p.locator('#clave').fill('Tutor2026!');await p.getByRole('button',{name:'Entrar a mi espacio'}).click();await p.locator('.layout').waitFor();return p;}
  async function action(p,accion,datos={}){const r=await p.evaluate(async({accion,datos})=>{const s=await (await fetch('/api/sesion')).json();const res=await fetch('/api/accion/'+accion,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({...datos,csrf:s.csrf})});return {status:res.status,body:await res.json()};},{accion,datos});assert.equal(r.status,200,accion+': '+JSON.stringify(r.body));return r;}
  async function nav(p,hash){await p.goto(url+'/#'+hash);await p.locator('.layout').waitFor();await p.waitForFunction(h=>location.hash==='#'+h,hash);}
  async function estado(p){return p.evaluate(async()=>await(await fetch('/api/estado')).json());}
  async function submit(p,f){const resp=p.waitForResponse(r=>r.url().includes('/api/accion/')&&r.request().method()==='POST');await f.locator('button[type="submit"]').click();const r=await resp;assert.equal(r.status(),200,JSON.stringify(await r.json()));await p.locator('#aviso.show').waitFor();checks++;}
  const coord=await login('coordinador','P001'),ana=await login('estudiante','E001'),maria=await login('tutor','T001'),luis=await login('estudiante','E002');
  await nav(coord,'periodos');
  let form=coord.locator('form[data-action="periodo-academico"]');await form.locator('[name="periodo"]').fill('PRUEBA-A');await form.locator('[name="inicio"]').fill(day(-30));await form.locator('[name="fin"]').fill(day(30));await submit(coord,form);
  form=coord.locator('form[data-action="periodo-academico"]');await form.locator('[name="periodo"]').fill('PRUEBA-B');await form.locator('[name="inicio"]').fill(day(40));await form.locator('[name="fin"]').fill(day(150));await submit(coord,form);
  form=coord.locator('form[data-action="politica-sanciones"]');await form.locator('[name="acuerdo"]').check();await submit(coord,form);
  async function reservar(n,alumno,finalizar){const fecha=day(n);await action(maria,'horario',{fecha,inicio:'08:00',fin:'09:00',modalidad:'PRESENCIAL',ubicacion:'Aula de prueba'});await action(alumno,'solicitar',{curso:'CC2006',tutor:'T001',horario:fecha+'|08:00|09:00',tema:'Herencia'});let e=await estado(alumno);const sol=e.solicitudes.at(-1);await action(maria,'aceptar',{solicitud:sol.id});e=await estado(maria);const ses=e.sesiones.find(s=>s.solicitud===sol.id);if(finalizar)await action(maria,'finalizar',{sesion:ses.id,temas:'Objetos',notas:'Buen avance',duracion:'1'});return {sol,ses};}
  const primera=await reservar(-5,ana,true);await reservar(-4,luis,true);
  const ausencia=await reservar(-3,ana,false);const anterior=await reservar(2,ana,false);
  await nav(ana,'historial');await ana.getByText('Valorar esta tutoría',{exact:true}).click();form=ana.locator('form[data-action="valorar"]');await form.locator('[name="puntuacion"]').selectOption('5');await form.locator('[name="comentario"]').fill('<img src=x onerror=alert(1)> Muy clara');await submit(ana,form);
  check(await ana.locator('article img').count()===0,'Comentario escapado en historial');
  const duplicate=await ana.evaluate(async({sesion})=>{const s=await(await fetch('/api/sesion')).json();return (await fetch('/api/accion/valorar',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({sesion,puntuacion:'2',csrf:s.csrf})})).status;},{sesion:''+primera.ses.id});check(duplicate===400,'Duplicado rechazado por HTTP');
  await nav(luis,'historial');await luis.getByText('Valorar esta tutoría',{exact:true}).click();form=luis.locator('form[data-action="valorar"]');await form.locator('[name="puntuacion"]').selectOption('1');await submit(luis,form);
  await nav(coord,'perfil?tutor=T001');check((await coord.locator('.rating-summary').innerText()).includes('3'),'Promedio 3/5');await coord.getByText('Registrar evaluación académica',{exact:true}).click();form=coord.locator('form[data-action="evaluacion-academica"]');await form.locator('[name="curso"]').selectOption('CC2006');await form.locator('[name="dominio"]').selectOption('4');await form.locator('[name="claridad"]').selectOption('5');await form.locator('[name="fundamento"]').fill('Dominio sólido y ejemplos claros.');await submit(coord,form);
  await nav(ana,'perfil?tutor=T001');check((await ana.locator('body').innerText()).includes('Dominio: 4/5'),'Evaluación separada visible');check((await ana.locator('.rating-summary').innerText()).includes('3'),'Evaluación no cambia promedio');check(await ana.locator('.card img').count()===0,'Texto escapado en perfil');await ana.screenshot({path:path.join(output,'perfil-valoraciones.png'),fullPage:true});
  await nav(ana,'solicitudes');const card=ana.locator('.request-card').filter({has:ana.locator('[name="solicitud"][value="'+ausencia.sol.id+'"]')}).filter({hasText:'Reportar ausencia'});await card.getByText('Reportar ausencia',{exact:true}).click();form=card.locator('form[data-action="reportar-ausencia"]');await form.locator('[name="motivo"]').fill('El tutor no llegó. <script>alert(1)</script>');await form.locator('[name="evidencia"]').fill('Esperé en el salón.');await submit(ana,form);
  await nav(maria,'ausencias');await maria.getByText('Registrar explicación',{exact:true}).click();form=maria.locator('form[data-action="explicar-ausencia"]');await form.locator('[name="contenido"]').fill('Tuve un inconveniente y no pude asistir.');await submit(maria,form);
  await nav(coord,'ausencias');await coord.getByText('Resolver reporte',{exact:true}).click();form=coord.locator('form[data-action="resolver-ausencia"]');await form.locator('[name="decision"]').selectOption('CONFIRMADO');await form.locator('[name="motivo"]').fill('Ausencia verificada con ambas partes.');await submit(coord,form);
  check((await estado(coord)).solicitudes.find(r=>r.id===ausencia.sol.id).estado==='INASISTENCIA','Cierre por inasistencia');
  check((await estado(coord)).solicitudes.find(r=>r.id===anterior.sol.id).afectadaPorSuspension,'Reserva previa afectada');
  await coord.screenshot({path:path.join(output,'revision-ausencias.png'),fullPage:true});
  await nav(luis,'buscar');check((await luis.locator('#lista-perfiles').innerText()).includes('Suspensión temporal activa'),'Búsqueda refleja suspensión');
  check(!(await luis.locator('body').innerText()).includes('Esperé en el salón'),'No revela evidencia a tercero');
  await nav(maria,'historial');check(await maria.locator('.history-card').count()===2,'Historial accesible al tutor suspendido');
  await nav(coord,'ausencias');form=coord.locator('form[data-action="resolver-reserva"]');await form.locator('[name="motivo"]').fill('La suspensión impide impartir la tutoría.');await submit(coord,form);
  await coord.getByText('Corregir decisión',{exact:true}).click();form=coord.locator('form[data-action="corregir-ausencia"]');await form.locator('[name="motivo"]').fill('Se verificó una justificación posterior.');await submit(coord,form);
  check((await estado(coord)).seguimiento.sanciones[0].vigente===false,'Corrección revoca efectos');
  await coord.locator('#filtros-reportes [name="estado"]').selectOption('CONFIRMADO');await coord.getByText('No hay reportes que coincidan',{exact:true}).waitFor();checks++;
  check(await coord.locator('#filtros-reportes [name="estado"]').inputValue()==='CONFIRMADO','Filtro se conserva vacío');
  for(const [p,ruta] of [[ana,'perfil?tutor=T001'],[coord,'ausencias'],[coord,'periodos'],[maria,'ausencias']]){
    await p.setViewportSize({width:390,height:844});await nav(p,ruta);
    check(await p.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1),'Sin desborde móvil '+ruta);
  }
  await coord.screenshot({path:path.join(output,'semestres-movil.png'),fullPage:true});
  check(errors.length===0,'Sin errores JS: '+errors.join('; '));
  console.log('CORRECTO: '+checks+' comprobaciones de navegador para cambios 7 y 8.');
  console.log('Capturas locales de pruebas: '+output);
 }finally{if(browser)await browser.close();server.kill();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
