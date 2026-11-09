/* Recorrido real con navegador y una base temporal, sin tocar los datos del proyecto. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {spawn} = require('node:child_process');
const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const root = path.resolve(__dirname, '../..');
const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'poo-ui-456-'));
const port = process.env.TEST_PORT || '18765';
const url = 'http://127.0.0.1:' + port;
const evidence = path.join(__dirname, 'resultados-456');
fs.mkdirSync(evidence, {recursive:true});
const server = spawn('java', ['-cp', 'bin'+path.delimiter+'lib/*', 'WebMain'], {
  cwd:root, env:{...process.env, PORT:port, DB_URL:'jdbc:h2:'+path.join(temporary,'db')+';MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE', DEMO_DATA:'true'}
});
let log='';server.stdout.on('data',d=>log+=d);server.stderr.on('data',d=>log+=d);
const errors=[];
let browser;
async function login(role,id) {
  const context=await browser.newContext({viewport:{width:1280,height:900}});
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  await page.goto(url);await page.locator('#rol').selectOption(role);
  await page.locator('#carnet').fill(id);await page.locator('#clave').fill('Tutor2026!');
  await page.locator('#login-form button[type=submit]').click();
  await page.locator('#logout').waitFor();return page;
}
let navigationId=0;
async function navigate(page,hash) {
  await page.goto(url+'/?prueba='+(++navigationId)+'#'+hash);await page.locator('#logout').waitFor();
}
async function submit(page,action) {
  await page.evaluate(()=>document.querySelector('#aviso').textContent='');
  const response=page.waitForResponse(r=>r.url().endsWith('/api/accion/'+action));
  await page.locator('form[data-action="'+action+'"] button[type=submit]').click();
  const r=await response;assert.equal(r.status(),200,await r.text());
  await page.waitForFunction(()=>document.querySelector('#aviso')?.textContent.includes('guardad') || document.querySelector('#aviso')?.textContent.includes('enviada'));
}
(async()=>{
 try {
  await new Promise((resolve,reject)=>{
    const deadline=setTimeout(()=>reject(new Error('Servidor no inició: '+log)),20000);
    server.stdout.on('data',()=>{if(log.includes('Tutorías disponible')){clearTimeout(deadline);resolve();}});
    server.on('exit',code=>reject(new Error('Servidor terminó '+code+': '+log)));
  });
  browser=await chromium.launch({channel:'chrome',headless:true});
  const maria=await login('tutor','T001');
  await navigate(maria,'disponibilidad');
  await maria.locator('#enlace').fill('https://meet.example.com/sala-privada?clave=456');
  await submit(maria,'enlace-permanente');
  for (const [day,mode,room] of [['01','EN_LINEA','https://meet.example.com/sala-privada?clave=456'],['02','PRESENCIAL','Salón B-205']]) {
    await maria.locator('#fecha').fill('2027-06-'+day);await maria.locator('#inicio').fill('10:00');await maria.locator('#fin').fill('11:30');
    await maria.locator('#modalidad-nuevo').selectOption(mode);
    if(mode==='EN_LINEA') assert.equal(await maria.locator('#ubicacion-nuevo').inputValue(),room);
    else await maria.locator('#ubicacion-nuevo').fill(room);
    await submit(maria,'horario');
    await maria.locator('article.card').filter({hasText:room}).first().waitFor();
  }
  await maria.screenshot({path:path.join(evidence,'01-disponibilidad.png'),fullPage:true});
  const coord=await login('coordinador','P001');
  await navigate(coord,'editar-tutor?tutor=T001');
  await coord.locator('#multiplicador').selectOption('3');await submit(coord,'editar-tutor');
  const ana=await login('estudiante','E001');
  const directory=await (await ana.request.get(url+'/api/estado')).json();
  assert.ok(!JSON.stringify(directory).includes('meet.example.com'));
  assert.equal((await ana.request.get(url+'/api/estudiante/E002')).status(),403);
  for (const [day,mode] of [['01','Virtual'],['02','Presencial']]) {
    await navigate(ana,'reservar?curso=CC2006&tutor=T001');
    await ana.locator('#horario').selectOption('2027-06-'+day+'|10:00|11:30');
    assert.equal(await ana.locator('#reserva-form [name=modalidad]').count(),0);
    assert.ok((await ana.locator('#resumen-reserva').innerText()).includes(mode));
    if(day==='02') assert.ok((await ana.locator('#resumen-reserva').innerText()).includes('Salón B-205'));
    await ana.locator('#tema').fill('Herencia');
    await ana.screenshot({path:path.join(evidence,'02-resumen-'+day+'.png'),fullPage:true});
    await submit(ana,'solicitar');await ana.locator('.request-card').first().waitFor();
  }
  assert.equal(await ana.locator('a[href="https://meet.example.com/sala-privada?clave=456"]').count(),1);
  assert.ok((await ana.locator('#contenido').innerText()).includes('Salón B-205'));
  await ana.screenshot({path:path.join(evidence,'03-solicitudes.png'),fullPage:true});
  const luis=await login('estudiante','E002');
  assert.ok(!(await (await luis.request.get(url+'/api/estado')).text()).includes('sala-privada'));
  assert.equal((await maria.request.get(url+'/api/estudiante/E002')).status(),403);
  assert.equal((await maria.request.get(url+'/api/estudiante/E001')).status(),200);
  assert.equal((await coord.request.get(url+'/api/estudiante/E002')).status(),200);
  await navigate(maria,'disponibilidad');
  await maria.locator('#enlace').fill('https://meet.example.com/nuevo');await submit(maria,'enlace-permanente');
  const booked=maria.locator('article.card').filter({hasText:'sala-privada'});
  assert.equal(await booked.locator('form').count(),0);
  assert.ok((await booked.innerText()).includes('Reservado'));
  await navigate(maria,'solicitudes');
  for(const id of ['1','2']) {
    const response=maria.waitForResponse(r=>r.url().endsWith('/api/accion/aceptar'));
    await maria.locator('[data-command="aceptar"][data-id="'+id+'"]').click();
    assert.equal((await response).status(),200);
    await maria.locator('[data-command="aceptar"][data-id="'+id+'"]').waitFor({state:'detached'});
  }
  await navigate(maria,'seguimiento?sesion=1');
  await maria.locator('#duracion').fill('1.5');await maria.locator('#temas').fill('Herencia y constructores');
  await maria.locator('#notas').fill('Resolviste los ejercicios');await maria.locator('#proximo').fill('Polimorfismo');
  await submit(maria,'finalizar');
  await navigate(maria,'compensacion');
  assert.ok((await maria.locator('#contenido').innerText()).includes('4.5'));
  assert.ok((await maria.locator('table').innerText()).includes('×3'));
  await maria.screenshot({path:path.join(evidence,'04-compensacion.png'),fullPage:true});
  await navigate(maria,'perfil-estudiante?estudiante=E001');
  await maria.locator('form[data-action="comentario-academico"]').waitFor();
  await maria.locator('#sesion').selectOption('1');
  await maria.locator('#contenido textarea').fill('Avance en herencia; recomendar más ejercicios de polimorfismo.');
  await submit(maria,'comentario-academico');
  await maria.getByText('Avance en herencia; recomendar más ejercicios de polimorfismo.',{exact:true}).waitFor();
  await maria.screenshot({path:path.join(evidence,'05-perfil-estudiante.png'),fullPage:true});
  const studentState=await (await ana.request.get(url+'/api/estado')).text();
  assert.ok(!studentState.includes('recomendar más ejercicios'));
  assert.ok(studentState.includes('Resolviste los ejercicios'));
  await navigate(maria,'perfil-estudiante?estudiante=E002');
  await maria.getByText('No tienes acceso a este perfil de estudiante.',{exact:true}).waitFor();
  await navigate(coord,'perfil-estudiante?estudiante=E001');
  await coord.getByText('Avance en herencia; recomendar más ejercicios de polimorfismo.',{exact:true}).waitFor();
  assert.equal(await coord.locator('form[data-action="comentario-academico"]').count(),0);
  await maria.setViewportSize({width:390,height:844});await navigate(maria,'compensacion');
  await maria.screenshot({path:path.join(evidence,'06-movil.png'),fullPage:true});
  assert.deepEqual(errors,[]);
  console.log('CORRECTO: recorrido de publicación, dos reservas, privacidad, factor ×3, finalización, comentarios y perfiles en Chrome.');
 } finally {
  if(browser) await browser.close();server.kill();
 }
})().catch(error=>{console.error(error);process.exitCode=1;});
