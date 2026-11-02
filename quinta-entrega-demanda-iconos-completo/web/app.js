'use strict';
const app=document.querySelector('#app');
let state=null,csrf='',demo=false,noticeTimer,cancelAction;
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const money=n=>'Q'+new Intl.NumberFormat('es-GT',{minimumFractionDigits:2,maximumFractionDigits:2}).format(n);
const date=v=>new Intl.DateTimeFormat('es-GT',{day:'numeric',month:'short',year:'numeric'}).format(new Date(v+'T12:00:00'));
const roleLabel={estudiante:'Estudiante',tutor:'Tutor',coordinador:'Coordinación'};
const statusLabel={PENDIENTE:'Pendiente',CONFIRMADA:'Confirmada',FINALIZADA:'Finalizada',CANCELADA:'Cancelada'};
// Iconos vectoriales locales para navegación y cursos.
const iconPaths={
 home:'<path d="m3 10 9-7 9 7v11h-6v-7H9v7H3Z"/>',
 search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
 document:'<path d="M5 3h14v18H5Z"/><path d="M9 8h6M9 12h6M9 16h4"/>',
 clock:'<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>',
 question:'<circle cx="12" cy="12" r="9"/><path d="M9 9a3 3 0 0 1 6 0c0 2-3 2-3 4m0 3h.01"/>',
 calendar:'<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 2v6m10-6v6M3 11h18M7 15h2m3 0h2m3 0h1M7 18h2m3 0h2"/>',
 cap:'<path d="m2 9 10-6 10 6-10 6Zm4 3v6q6 5 12 0v-6m4-3v10"/>',
 users:'<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3Zm13-16a3 3 0 0 1 0 6m2 3q4 1 4 7"/>',
 user:'<circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2Z"/>',
 book:'<path d="M12 5Q7 2 2 4v15q5-2 10 1 5-3 10-1V4q-5-2-10 1Zm0 0v15"/>',
 nodes:'<circle cx="12" cy="4" r="3"/><circle cx="4" cy="18" r="3"/><circle cx="20" cy="18" r="3"/><path d="m10 7-5 8m9-8 5 8M7 18h10"/>',
 code:'<path d="m8 5-6 7 6 7m8-14 6 7-6 7M14 3l-4 18"/>',
 math:'<path d="M6 2v8M2 6h8m4 0h8M3 15l6 6m0-6-6 6m11-3h8"/><circle cx="18" cy="14" r=".5"/><circle cx="18" cy="22" r=".5"/>',
 atom:'<circle cx="12" cy="12" r="1"/><ellipse cx="12" cy="12" rx="10" ry="4"/><ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(60 12 12)"/><ellipse cx="12" cy="12" rx="10" ry="4" transform="rotate(120 12 12)"/>',
 flask:'<path d="M9 3h6m-5 0v7L4 19q-1 2 2 2h12q3 0 2-2l-6-9V3M7 15h10"/><path d="M10 18h.01M14 17h.01"/>',
 leaf:'<path d="M20 3C9 2 3 7 3 13a7 7 0 0 0 7 7c7 0 11-8 10-17ZM3 21 15 9m-6 6v-5m0 5h5"/>',
 globe:'<circle cx="12" cy="12" r="9"/><ellipse cx="12" cy="12" rx="4" ry="9"/><path d="M3 12h18M5 7h14M5 17h14"/>',
 landmark:'<path d="m3 8 9-5 9 5ZM3 21h18M4 18h16M6 8v10m6-10v10m6-10v10"/>',
 palette:'<path d="M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 1-4c-1-1 0-3 2-3h2a3 3 0 0 0 3-3 9 9 0 0 0-9-8Z"/><circle cx="7" cy="10" r="1"/><circle cx="11" cy="7" r="1"/><circle cx="16" cy="8" r="1"/><circle cx="6" cy="15" r="1"/>',
 music:'<path d="M9 18V5l12-2v13M9 9l12-2"/><ellipse cx="6" cy="18" rx="3" ry="3"/><ellipse cx="18" cy="16" rx="3" ry="3"/>',
 money:'<rect x="2" y="5" width="20" height="14" rx="2"/><circle cx="12" cy="12" r="4"/><path d="M5 9v6m14-6v6"/>',
 pencil:'<path d="m4 16-1 5 5-1L21 7l-6-5Zm10-12 6 6M4 16l4 4"/>',
 lock:'<rect x="5" y="10" width="14" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4m-4 5v3"/>',
 check:'<circle cx="12" cy="12" r="9"/><path d="m7 12 3 3 7-7"/>',
 spark:'<path d="M12 1q1 10 10 11-9 1-10 11Q11 13 2 12 11 11 12 1Z"/>'
};
const icon=(name,cls='')=>`<svg class="ui-icon ${cls}" data-icon="${esc(name)}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${iconPaths[name]||iconPaths.book}</svg>`;
const sticker=name=>`<span class="sticker" aria-hidden="true">${icon(name)}</span>`;
const navIcon=id=>icon(navArt[id]);
const navArt={inicio:'home',buscar:'search',solicitudes:'document',historial:'clock',preguntas:'question',sesiones:'calendar',disponibilidad:'calendar',compensacion:'money',tutores:'users',estudiantes:'user',cursos:'book',progreso:'nodes',demanda:'document',profesor:'cap'};
const courseArt=name=>/matem|álgebra|algebra|cálculo|calculo/i.test(name)?'math':/algorit|estructura/i.test(name)?'nodes':/program|inform|comput/i.test(name)?'code':'book';
const courseIcons=[['book','Libro'],['code','Programación'],['math','Matemáticas'],['nodes','Algoritmos'],['atom','Física'],['flask','Química'],['leaf','Biología'],['globe','Idiomas'],['landmark','Historia'],['palette','Arte'],['music','Música'],['money','Economía']];
const courseIcon=c=>courseIcons.some(([id])=>id===c.icono)?c.icono:courseArt(c.nombre);
function courseIconPicker(){
 return `<fieldset class="course-icon-picker" aria-describedby="icono-ayuda"><legend>Icono del curso</legend><p id="icono-ayuda" class="muted small">Elige la imagen que mejor represente la clase. También la verán los estudiantes.</p><div class="course-icon-options">${courseIcons.map(([id,label])=>`<label class="course-icon-option" for="icono-${id}"><input type="radio" id="icono-${id}" name="icono" value="${id}" ${id==='book'?'checked':''} required><span class="course-icon-choice">${icon(id)}<span>${label}</span><span class="course-icon-selected" aria-hidden="true">✓</span></span></label>`).join('')}</div></fieldset>`;
}
const headingArt=title=>/contraseña|cuenta/i.test(title)?'lock':/pregunta/i.test(title)?'question':/horario|disponibilidad|sesiones|reserva/i.test(title)?'calendar':/tiempo|compens/i.test(title)?'money':/tutores|comunidad/i.test(title)?'users':/estudiantes/i.test(title)?'user':/registro|avance|seguimiento|paso/i.test(title)?'pencil':'book';
const mascot=(explorer=false)=>`<div class="mascot-scene ${explorer?'is-explorer':''}" aria-hidden="true"><span class="mascot-backdrop"></span><span class="mascot-spark">${icon('spark')}</span><span class="mascot-lines"></span><span class="mascot-frame"><span class="mascot-cutout"></span></span></div>`;
const logo=()=>'<a class="logo" href="#inicio" aria-label="Tutorías UVG, ir al inicio"><span class="brand-lockup"><span class="uvg-logo" role="img" aria-label="Universidad del Valle de Guatemala"></span><span class="brand-slogan">Excelencia que trasciende</span></span><span class="brand-name">Tutorías<small>Apoyo académico</small></span></a>';
const badge=s=>`<span class="badge ${s.toLowerCase()}">${statusLabel[s]}</span>`;
const empty=(title,text,link='')=>`<div class="empty"><span class="empty-icon" aria-hidden="true">${icon('book')}</span><h3>${esc(title)}</h3><p>${esc(text)}</p>${link}</div>`;
const link=(url,text,cls='button')=>`<a class="${cls}" href="${esc(url)}">${esc(text)}</a>`;
const hidden=(name,value)=>`<input type="hidden" name="${name}" value="${esc(value)}">`;
const field=(name,label,type='text',value='',extra='')=>`<div class="field"><label for="${name}">${label}</label><input id="${name}" name="${name}" type="${type}" value="${esc(value)}" ${extra} required></div>`;
const textarea=(name,label,required=true)=>`<div class="field"><label for="${name}">${label}</label><textarea id="${name}" name="${name}" maxlength="2000" ${required?'required':''}></textarea></div>`;
const errorBox=()=>'<p class="form-error" role="alert"></p>';
const form=(action,content,button)=>`<form data-action="${action}">${content}${errorBox()}<button type="submit">${button}</button></form>`;
const options=(items,id,name,first)=>`<option value="">${first}</option>`+items.map(x=>`<option value="${esc(x[id])}">${esc(x[name])}</option>`).join('');
function select(name,label,items,id,key,first='Selecciona una opción'){return `<div class="field"><label for="${name}">${label}</label><select id="${name}" name="${name}" required>${options(items,id,key,first)}</select></div>`;}
async function api(path,data){
  const opts=data?{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({...data,csrf})}:{};
  const r=await fetch('/api/'+path,opts);const body=await r.json();
  if(!r.ok){const e=new Error(body.error||'No se pudo completar la operación.');e.status=r.status;throw e;}
  if(body.csrf)csrf=body.csrf;return body;
}
function notify(text){const n=document.querySelector('#aviso');n.textContent=text;n.classList.add('show');clearTimeout(noticeTimer);noticeTimer=setTimeout(()=>{n.classList.remove('show');n.textContent='';},6500);}
async function load(){state=await api('estado');}
function route(){const raw=location.hash.slice(1)||'inicio';const [page,query]=raw.split('?');return {page,q:new URLSearchParams(query||'')};}
function heading(title,description,action=''){return `<header class="page-heading"><div class="heading-copy"><p class="eyebrow">TU ESPACIO DE APOYO</p><h1>${title}</h1><p class="muted">${description}</p></div>${action}<span class="heading-art" aria-hidden="true">${sticker(headingArt(title))}</span></header>`;}
function login(){
  state=null;
  app.innerHTML=`<div class="login">${logo()}<main id="contenido" class="login-columns"><section class="login-story"><p class="eyebrow">APRENDER ES MEJOR EN COMPAÑÍA</p><h1>Un poco de apoyo.<br>Un gran siguiente paso.</h1><p class="intro">Encuentra a tu tutor, elige un horario y sigue avanzando. Todo tu acompañamiento académico, en un solo lugar.</p><div class="steps"><div class="step"><b>01</b>Encuentra tu curso</div><div class="step"><b>02</b>Reserva un horario</div><div class="step"><b>03</b>Revisa tu progreso</div></div>${mascot()}</section><section class="login-card"><h2>Bienvenido a Tutorías</h2><p class="muted">Ingresa a tu espacio de estudiante, tutor o coordinación.</p><form id="login-form"><div class="field"><label for="rol">Tu perfil</label><select name="rol" id="rol"><option value="estudiante">Estudiante</option><option value="tutor">Tutor</option><option value="coordinador">Profesor o coordinador</option></select></div>${field('carnet','Carné o identificador','text','','autocomplete="username" maxlength="80"')}${field('clave','Contraseña','password','','autocomplete="current-password" maxlength="128"')}${errorBox()}<button class="full" type="submit">Entrar a mi espacio <span aria-hidden="true">→</span></button></form>${demo?'<div class="demo"><p><strong>Explora la demostración</strong></p><p class="muted">Cuentas de ejemplo. Contraseña: <strong>Tutor2026!</strong></p><div class="demo-users"><button class="secondary" data-demo="estudiante,E001">Ana · estudiante</button><button class="secondary" data-demo="estudiante,E002">Luis · último año</button><button class="secondary" data-demo="tutor,T001">María · tutor</button><button class="secondary" data-demo="tutor,T002">Diego · tutor</button><button class="secondary" data-demo="coordinador,P001">Coordinación</button></div></div>':''}${studentRegistration()}</section></main><footer class="login-footer">Proyecto académico · Universidad del Valle de Guatemala · Programación Orientada a Objetos</footer></div>`;
}
function studentRegistration(){
  return `<details id="registro-estudiante"><summary>¿Eres nuevo? Registrarme como estudiante</summary><p class="muted small">Crea tu cuenta para solicitar tutorías.</p><form id="registro-form">
    <div class="field"><label for="registro-nombre">Nombre completo</label><input id="registro-nombre" name="nombre" autocomplete="name" maxlength="200" required></div>
    <div class="field-row">
      <div class="field"><label for="registro-carnet">Carné</label><input id="registro-carnet" name="carnet" autocomplete="username" pattern="[A-Za-z0-9_-]+" maxlength="80" required></div>
      <div class="field"><label for="registro-anio">Año de carrera</label><input id="registro-anio" name="anio" type="number" min="1" max="10" value="1" required></div>
    </div>
    <div class="field"><label for="registro-correo">Correo electrónico</label><input id="registro-correo" name="correo" type="email" autocomplete="email" maxlength="200" required></div>
    <div class="field"><label for="registro-clave">Contraseña para tu cuenta</label><input id="registro-clave" name="clave" type="password" autocomplete="new-password" minlength="8" maxlength="128" aria-describedby="registro-clave-ayuda" required><small id="registro-clave-ayuda">Entre 8 y 128 caracteres.</small></div>
    <div class="field"><label for="registro-confirmacion">Confirmar contraseña</label><input id="registro-confirmacion" name="confirmacion" type="password" autocomplete="new-password" minlength="8" maxlength="128" required></div>
    ${errorBox()}<button class="full" type="submit">Crear mi cuenta de estudiante</button>
  </form></details>`;
}
async function registerStudent(f,data){
  const button=f.querySelector('button[type=submit]'),box=f.querySelector('.form-error');
  box.textContent='';button.disabled=true;
  try{
    if(data.clave!==data.confirmacion)throw new Error('Las contraseñas no coinciden.');
    await api('registro-estudiante',data);
    login();
    document.querySelector('#carnet').value=data.carnet.trim();
    document.querySelector('#clave').focus();
    notify('Cuenta creada correctamente. Ingresa tu contraseña para iniciar sesión como estudiante.');
  }catch(e){box.textContent=e.message;box.tabIndex=-1;box.focus();}
  finally{button.disabled=false;}
}
function navigation(){
  if(state.usuario.rol==='estudiante')return [['inicio','⌂','Mi espacio'],['buscar','⌕','Buscar tutoría'],['solicitudes','▤','Mis solicitudes'],['historial','◷','Historial'],['preguntas','◌','Mis preguntas']];
  if(state.usuario.rol==='tutor')return [['inicio','⌂','Mi espacio'],['solicitudes','▤','Solicitudes'],['sesiones','◷','Sesiones'],['disponibilidad','▦','Disponibilidad'],['historial','✓','Historial'],['compensacion','◇','Compensación']];
  return [['inicio','⌂','Resumen'],['tutores','◉','Tutores'],['estudiantes','♧','Estudiantes'],['cursos','▤','Cursos'],['solicitudes','▤','Solicitudes'],['progreso','↗','Progreso'],['demanda','▥','Demanda'],['historial','◷','Historial general'],['profesor','◇','Impartir tutorías']];
}
function shell(content,page){
  const u=state.usuario;app.innerHTML=`<div class="layout" data-page="${esc(page)}"><aside class="sidebar">${logo()}<p class="nav-label">${roleLabel[u.rol].toUpperCase()}</p><nav aria-label="Navegación principal">${navigation().map(([id,icon,label])=>`<a href="#${id}" class="${id===page?'active':''}" ${id===page?'aria-current="page"':''}><span class="nav-icon" aria-hidden="true">${navIcon(id)}</span>${label}</a>`).join('')}</nav><div class="sidebar-bottom">Un paso a la vez,<br>con el apoyo adecuado.<br>${link('#ayuda','Guía de uso','text-button')}</div></aside><div class="workspace"><header class="topbar"><small>PROGRAMA DE ASESORÍAS ACADÉMICAS</small><div class="identity"><a class="avatar" href="#cuenta" aria-label="Mi cuenta">${esc(u.nombre.charAt(0))}</a><span>${esc(u.nombre)}<small>${roleLabel[u.rol]} · ${esc(u.carnet)}</small></span><button class="text-button" id="logout">Salir</button></div></header><main id="contenido" class="main" tabindex="-1">${content}${page==='ayuda'?mascot(true):''}</main></div></div>`;
}
function currentTutor(){return state.tutores.find(t=>t.carnet===state.usuario.carnet);}
function mine(list=state.solicitudes){return state.usuario.rol==='coordinador'?list.filter(s=>s.tutor===state.usuario.carnet):list;}
function sessionRequests(finalized){return state.sesiones.filter(s=>s.finalizada===finalized).map(s=>({session:s,request:state.solicitudes.find(r=>r.id===s.solicitud)})).filter(x=>x.request&&(finalized||x.request.estado==='CONFIRMADA'));}
function home(){
 const student=state.usuario.rol==='estudiante',coordinator=state.usuario.rol==='coordinador';
 const pending=state.solicitudes.filter(s=>s.estado==='PENDIENTE').length,confirmed=state.solicitudes.filter(s=>s.estado==='CONFIRMADA').length,finished=state.sesiones.filter(s=>s.finalizada).length;
 const title=student?'Tu próximo logro<br>empieza con una tutoría.':coordinator?'El apoyo adecuado,<br>para cada estudiante.':'Comparte lo que sabes.<br>Acompaña su progreso.';
 const subtitle=student?'Elige el curso que necesitas reforzar y encuentra un horario que funcione para ti.':coordinator?'Organiza la oferta de asesorías y conoce cómo avanzan los estudiantes.':'Organiza tus solicitudes, prepara tus sesiones y deja un seguimiento útil.';
 const cta=student?link('#buscar','Encontrar una tutoría →'):coordinator?link('#tutores','Gestionar tutores →'):link('#solicitudes','Revisar solicitudes →');
 let b=`<section class="hero"><div><p class="eyebrow">HOLA, ${esc(state.usuario.nombre.split(' ')[0].toUpperCase())}</p><h1>${title}</h1><p>${subtitle}</p>${cta}</div><div class="hero-art" aria-hidden="true">${mascot()}</div></section><section class="stats" aria-label="Resumen de actividad">${[[pending,'Solicitudes pendientes'],[confirmed,'Sesiones por realizar'],[finished,'Tutorías finalizadas']].map(([n,label])=>`<div class="stat">${sticker(label==='Solicitudes pendientes'?'document':label==='Sesiones por realizar'?'calendar':'cap')}<div class="value">${n.toString().padStart(2,'0')}</div><div class="label">${label}</div></div>`).join('')}</section>`;
 b+=`<div class="section-heading"><h2>${student?'¿Qué quieres reforzar?':coordinator?'La comunidad en números':'Tus próximas sesiones'}</h2>${link(student?'#buscar':coordinator?'#estudiantes':'#sesiones','Ver todo →','text-button')}</div>`;
 if(student)b+=courseCards(state.cursos.slice(0,3));
 else if(coordinator)b+=`<div class="grid three">${[[state.estudiantes.length,'Estudiantes registrados'],[state.tutores.filter(t=>t.activo!==false).length,'Tutores y profesores'],[state.cursos.length,'Cursos registrados']].map(([n,l])=>`<article class="card community-card">${sticker(l==='Estudiantes registrados'?'users':l==='Tutores y profesores'?'user':'book')}<h2>${n}</h2><p class="muted">${l}</p></article>`).join('')}</div>`;
 else{const sessions=mine().filter(r=>r.estado==='CONFIRMADA');b+=sessions.length?`<div class="grid">${sessions.slice(0,4).map(requestCard).join('')}</div>`:empty('Todo listo para tu próxima tutoría','Cuando aceptes una solicitud, la sesión aparecerá aquí.');}
 b+=`<p class="subtle">${demo?'Estás usando datos de demostración. ':''}Los cambios se guardan al confirmar cada operación.</p>`;return b;
}
function courseCards(courses){return `<div class="grid three">${courses.map(c=>{const tutors=state.tutores.filter(t=>t.cursos.includes(c.codigo)&&t.horarios.some(h=>h.disponible));return `<article class="card course-card">${sticker(courseIcon(c))}<span class="course-code">${esc(c.codigo)}</span><h3>${esc(c.nombre)}</h3><p class="muted small">${tutors.length} ${tutors.length===1?'tutor disponible':'tutores disponibles'}</p>${link('#reservar?curso='+encodeURIComponent(c.codigo),tutors.length?'Ver tutores y horarios →':'Consultar opciones →','text-button')}</article>`;}).join('')}</div>`;}
function search(){return heading('Encuentra tu próxima tutoría','Busca un curso y elige el apoyo que necesitas.')+'<div class="filterbar"><div class="field"><label for="buscar-curso">Nombre o código del curso</label><input type="search" id="buscar-curso" placeholder="Por ejemplo, Programación"></div></div><div id="lista-cursos">'+courseCards(state.cursos)+'</div>';}
function booking(q){
 if(!state.cursos.length)return heading('Solicitar tutoría','Consulta la oferta académica.')+empty('Aún no hay cursos','Coordinación debe registrar los cursos y aprobar a los tutores.');
 return heading('Reserva un espacio para aprender','Elige tu curso, tu tutor y el horario. Revisa el costo antes de confirmar.',link('#buscar','Volver a cursos','button secondary'))+`<div class="booking-grid"><section class="card"><form data-action="solicitar" id="reserva-form">${select('curso','1. ¿En qué curso necesitas apoyo?',state.cursos,'codigo','nombre','Selecciona un curso')}<div class="field"><label for="tutor">2. Elige un tutor</label><select id="tutor" name="tutor" required><option value="">Primero selecciona un curso</option></select></div><button type="button" class="text-button" id="alternativa">Ayúdame a encontrar un tutor</button><p id="recomendacion" class="muted small" role="status"></p><div class="field"><label for="horario">3. Elige un horario</label><select id="horario" name="horario" required><option value="">Primero selecciona un tutor</option></select></div>${textarea('tema','4. ¿Qué tema quieres trabajar?')}<p class="muted small">Por ejemplo: herencia y constructores. La solicitud reservará el bloque completo.</p>${errorBox()}<button type="submit">Confirmar solicitud</button></form></section><aside class="summary" aria-label="Resumen de la reserva"><p class="eyebrow">TU TUTORÍA</p><h2>Antes de confirmar</h2><div id="resumen-reserva"><p>Selecciona un curso, tutor y horario para conocer los detalles.</p></div></aside></div>`;
}
function updateTutors(preselected=''){
 const c=document.querySelector('#curso'),t=document.querySelector('#tutor');if(!c||!t)return;
 const available=state.tutores.filter(x=>x.cursos.includes(c.value)&&x.horarios.some(h=>h.disponible));
 t.innerHTML=options(available,'carnet','nombre',available.length?'Selecciona un tutor':'No hay tutores disponibles');if(available.some(x=>x.carnet===preselected))t.value=preselected;
 document.querySelector('#recomendacion').textContent=available.length?'':'No hay horarios compatibles en este curso. Prueba otro curso o consulta a coordinación.';
 updateHours();
}
function updateHours(){
 const t=state.tutores.find(t=>t.carnet===document.querySelector('#tutor')?.value),sel=document.querySelector('#horario');if(!sel)return;
 sel.innerHTML='<option value="">Selecciona un horario</option>'+(t?t.horarios.filter(h=>h.disponible).map(h=>`<option value="${esc(h.id)}">${date(h.fecha)} · ${h.inicio} a ${h.fin}</option>`).join(''):'');updateSummary();
}
function updateSummary(){
 const c=state.cursos.find(c=>c.codigo===document.querySelector('#curso')?.value),t=state.tutores.find(t=>t.carnet===document.querySelector('#tutor')?.value),h=t?.horarios.find(h=>h.id===document.querySelector('#horario')?.value),panel=document.querySelector('#resumen-reserva');if(!panel)return;
 panel.innerHTML=`<dl><dt>Curso</dt><dd>${esc(c?.nombre||'Por seleccionar')}</dd><dt>Tutor</dt><dd>${esc(t?.nombre||'Por seleccionar')}</dd><dt>Horario</dt><dd>${h?date(h.fecha)+' · '+h.inicio+' a '+h.fin:'Por seleccionar'}</dd></dl><hr><span class="muted small">PRECIO TOTAL</span><div class="price">${h?money(h.duracion*t.tarifa):'—'}</div><p class="small">${t?.beca?'Esta tutoría es gratuita y acredita horas beca al tutor.':h?h.duracion+' horas × '+money(t.tarifa)+' por hora.':'El precio aparecerá al elegir el horario.'}</p><p class="small muted">El tutor confirmará tu solicitud. Podrás revisar el estado en Mis solicitudes.</p>`;
}
function questionList(id){const qs=state.preguntas.filter(q=>q.solicitud===id);return qs.length?`<details><summary>Preguntas de esta tutoría (${qs.length})</summary>${qs.map(q=>`<div class="question"><strong>${esc(q.estudiante)}</strong><br>${esc(q.contenido)}</div>`).join('')}</details>`:'';}
function requestCard(r){
 const isStudent=state.usuario.rol==='estudiante',canTutor=r.tutor===state.usuario.carnet&&(state.usuario.rol==='tutor'||state.usuario.rol==='coordinador'),active=['PENDIENTE','CONFIRMADA'].includes(r.estado);
 let actions='';
 if(canTutor&&r.estado==='PENDIENTE')actions+=`<button data-command="aceptar" data-id="${r.id}">Aceptar tutoría</button>`;
 if(active&&(isStudent||canTutor))actions+=`<button class="secondary" data-command="cancelar" data-id="${r.id}">Cancelar</button>`;
 const ses=state.sesiones.find(s=>s.solicitud===r.id&&!s.finalizada);
 if(canTutor&&r.estado==='CONFIRMADA'&&ses)actions+=link('#seguimiento?sesion='+ses.id,'Registrar seguimiento');
 return `<article class="card request-card"><div class="card-top"><span class="course-code">${esc(r.curso)} · #${r.id}</span>${badge(r.estado)}</div><h3>${esc(r.nombreCurso)}</h3><p class="muted small">${isStudent?'Tutor: '+esc(r.nombreTutor):'Estudiante: '+esc(r.nombreEstudiante)}${state.usuario.rol==='coordinador'?' · Tutor: '+esc(r.nombreTutor):''}</p><div class="meta"><span>${date(r.fecha)}</span><span>${r.inicio} – ${r.fin}</span><strong>${money(r.precio)}</strong></div><div class="detail"><h4>Tema solicitado</h4><p>${esc(r.tema)}</p></div>${actions?'<div class="actions">'+actions+'</div>':''}${questionList(r.id)}${isStudent&&active?`<details><summary>Agregar una pregunta</summary>${form('preguntar',hidden('solicitud',r.id)+`<div class="field"><label for="pregunta-${r.id}">Tu duda sobre la tutoría</label><textarea id="pregunta-${r.id}" name="contenido" required maxlength="2000"></textarea></div>`,'Guardar pregunta')}</details>`:''}</article>`;
}
function requests(){
 const student=state.usuario.rol==='estudiante';return heading(student?'Mis solicitudes':'Solicitudes de tutoría','Revisa los temas, horarios y el estado de cada solicitud.')+`<div class="filterbar"><div class="field"><label for="filtro-estado">Estado</label><select id="filtro-estado"><option value="">Todos los estados</option>${Object.entries(statusLabel).map(([v,l])=>`<option value="${v}">${l}</option>`).join('')}</select></div></div><div id="lista-solicitudes">${requestList(state.solicitudes)}</div>`;
}
function requestList(list){return list.length?`<div class="grid">${[...list].reverse().map(requestCard).join('')}</div>`:empty('No hay solicitudes para mostrar','Las nuevas solicitudes aparecerán aquí.',state.usuario.rol==='estudiante'?link('#buscar','Buscar una tutoría'):'');}
function history(filter=''){
 const list=sessionRequests(true).filter(x=>!filter||x.request.estudiante===filter);
 return list.length?`<div class="grid">${list.map(({session:s,request:r})=>`<article class="card history-card"><div class="card-top"><span class="course-code">${esc(r.curso)} · Sesión ${s.id}</span>${badge('FINALIZADA')}</div><h3>${esc(r.nombreCurso)}</h3><p class="muted small">${esc(r.nombreEstudiante)} con ${esc(r.nombreTutor)}</p><div class="meta"><span>${date(r.fecha)}</span><span>${s.duracion} horas</span><span>${money(r.precio)}</span></div><div class="detail"><h4>Temas vistos</h4><p>${esc(s.temas)}</p><h4>Notas del tutor</h4><p>${esc(s.notas)}</p><h4>Para tu próxima tutoría</h4><p>${esc(s.proximo||'No se indicó un próximo tema.')}</p></div></article>`).join('')}</div>`:empty('Tu historial empieza aquí','Las tutorías aparecerán después de que el tutor registre el seguimiento y finalice la sesión.');
}
function questions(){
 const active=state.solicitudes.filter(r=>['PENDIENTE','CONFIRMADA'].includes(r.estado)).map(r=>({...r,label:r.nombreCurso+' · '+date(r.fecha)+' · '+r.nombreTutor}));
 return heading('Tus preguntas, a la mano','Comparte con tu tutor las dudas que quieres trabajar.')+`<div class="grid"><section class="card"><h2>Escribe una pregunta</h2>${active.length?form('preguntar',select('solicitud','Tutoría relacionada',active,'id','label')+textarea('contenido','¿Qué te gustaría preguntar?'),'Guardar pregunta'):empty('Necesitas una solicitud activa','Primero solicita una tutoría para compartir tus dudas.',link('#buscar','Buscar tutoría'))}</section><section><h2>Preguntas registradas</h2>${state.preguntas.length?state.preguntas.map(q=>{const r=state.solicitudes.find(r=>r.id===q.solicitud);return `<article class="card"><p class="muted small">${esc(r?.nombreCurso)} · Solicitud #${q.solicitud}</p><p>${esc(q.contenido)}</p></article>`;}).join(''):empty('Todavía no has escrito preguntas','Tus dudas quedarán vinculadas a cada tutoría.')}</section></div>`;
}
function sessions(){const list=mine().filter(r=>r.estado==='CONFIRMADA');return heading('Sesiones por realizar','Al terminar una tutoría, registra los temas y las notas para tu estudiante.')+requestList(list);}
function followup(q){const s=state.sesiones.find(s=>String(s.id)===q.get('sesion')),r=state.solicitudes.find(r=>r.id===s?.solicitud);if(!s||!r||r.tutor!==state.usuario.carnet||r.estado!=='CONFIRMADA')return empty('Esta sesión no está disponible','Consulta tus sesiones pendientes.',link('#sesiones','Ver sesiones'));
return heading('Registra el siguiente paso',`${esc(r.nombreEstudiante)} · ${esc(r.nombreCurso)} · ${date(r.fecha)}`)+`<section class="card form-panel">${form('finalizar',hidden('sesion',s.id)+textarea('temas','¿Qué temas trabajaron?')+textarea('notas','Notas y observaciones para el estudiante')+textarea('proximo','Tema recomendado para la próxima tutoría (opcional)',false)+'<p class="muted small">Al finalizar se actualizará el historial y se acreditará la compensación correspondiente una sola vez.</p>','Guardar y finalizar sesión')}</section>`;}
function availability(){const t=currentTutor();return heading('Organiza tu disponibilidad','Publica bloques completos. Tus horarios se comparten entre todos tus cursos.')+`<div class="grid"><section class="card"><h2>Agregar un horario</h2>${form('horario',field('fecha','Fecha','date')+'<div class="field-row">'+field('inicio','Hora de inicio','time')+field('fin','Hora de finalización','time')+'</div>','Publicar horario')}<div class="detail"><h4>Cursos asignados</h4><p>${esc(t.cursos.map(id=>state.cursos.find(c=>c.codigo===id)?.nombre).join(', ')||'Aún no tienes cursos autorizados. Solicita aprobación a coordinación.')}</p></div></section><section><h2>Tus horarios</h2>${t.horarios.length?`<div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Horario</th><th>Estado</th></tr></thead><tbody>${t.horarios.map(h=>`<tr><td>${date(h.fecha)}</td><td>${h.inicio} – ${h.fin}</td><td>${h.disponible?'Disponible':'Reservado'}</td></tr>`).join('')}</tbody></table></div>`:empty('Tu agenda está abierta','Agrega tu primer horario para que puedan reservar contigo.')}</section></div>`;}
function compensation(){const t=currentTutor(),list=sessionRequests(true).filter(x=>x.request.tutor===t.carnet);return heading('Tu tiempo también cuenta','Consulta las horas y la compensación de las tutorías que has finalizado.')+`<div class="stats"><div class="stat">${sticker('clock')}<div class="value">${t.horasBeca}</div><div class="label">Horas beca acumuladas</div></div><div class="stat">${sticker('money')}<div class="value">${money(t.remuneracion)}</div><div class="label">Remuneración acumulada</div></div><div class="stat">${sticker('check')}<div class="value">${list.length}</div><div class="label">Sesiones finalizadas</div></div></div><p class="notice">${t.beca?'Tu perfil acredita horas beca. Tus tutorías no tienen costo.':'Monto correspondiente a tus tutorías. El sistema calcula la compensación; no procesa pagos bancarios.'}</p><section class="card"><h2>Detalle de sesiones</h2>${list.length?`<div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Curso</th><th>Horas</th><th>Monto</th></tr></thead><tbody>${list.map(({session:s,request:r})=>`<tr><td>${date(r.fecha)}</td><td>${esc(r.nombreCurso)}</td><td>${s.duracion}</td><td>${r.beca?'Horas beca':money(r.precio)}</td></tr>`).join('')}</tbody></table></div>`:empty('Aún no hay compensación registrada','Se calculará cuando finalices tu primera sesión.')}</section>`;}
function personForm(type){let b=field('nombre','Nombre completo','text','','maxlength="200"')+'<div class="field-row">'+field('carnet','Carné','text','','pattern="[A-Za-z0-9_-]+" maxlength="80"')+field('anio','Año de carrera','number','1','min="1" max="10"')+'</div>'+field('correo','Correo electrónico','email','','maxlength="200"')+field('clave','Contraseña inicial','password','','minlength="8" maxlength="128" autocomplete="new-password"');if(type==='tutor')b+='<div class="field"><label class="check-label"><input type="checkbox" name="beca" id="beca">Recibe horas beca</label></div>'+field('tarifa','Tarifa por hora en quetzales','number','0','min="0" step="0.01"');return form(type,b,'Registrar '+type);}
function tutors(){
 const active=state.tutores.filter(t=>t.activo!==false),retired=state.tutores.filter(t=>t.activo===false);
 return heading('Tutores que acompañan','Registra tutores, edita sus datos y autoriza los cursos que pueden impartir.')+
 `<div class="grid"><section class="card"><h2>Aprobar tutor por curso</h2>${state.cursos.length&&active.length?form('aprobar',select('tutor','Tutor',active,'carnet','nombre')+select('curso','Curso',state.cursos,'codigo','nombre'),'Aprobar tutor'):empty('Registra un curso y un tutor','Después podrás autorizar las tutorías.')}</section><section class="card"><h2>Ampliar la comunidad</h2><p class="muted">Cada nuevo tutor recibe una cuenta y necesita autorización por curso.</p><details><summary>Registrar un nuevo tutor</summary>${personForm('tutor')}</details></section></div>
 <div class="section-heading"><h2>Tutores y profesores activos</h2></div><div class="grid">${active.map(t=>`<article class="card"><div class="card-top"><h3>${esc(t.nombre)}</h3><span class="badge">${t.profesor?'Profesor':t.aprobado?'Aprobado':'Sin aprobar'}</span></div><p class="muted small">${esc(t.carnet)} · ${esc(t.correo)}</p><p class="small">${t.beca?'Horas beca · sin costo':money(t.tarifa)+' por hora'} · Año ${t.anio}</p><div class="detail"><h4>Cursos autorizados</h4><p>${esc(t.cursos.map(id=>state.cursos.find(c=>c.codigo===id)?.nombre).join(', ')||'Ninguno todavía')}</p></div>
 <div class="actions">${link('#editar-tutor?tutor='+encodeURIComponent(t.carnet),'Editar datos y pago','button secondary')}<button type="button" class="danger" data-command="eliminar-tutor" data-id="${esc(t.carnet)}">Quitar ${t.profesor?'profesor':'tutor'}</button></div>
 ${tutorHistory(t)}</article>`).join('')||empty('No hay tutores activos','Registra un tutor para ampliar la oferta.')}</div>
 ${retired.length?`<section class="card"><details><summary>Personal retirado (${retired.length})</summary><p class="muted small">Se conserva su historial. Estos perfiles ya no aparecen en la oferta de tutorías.</p>${retired.map(t=>`<article><h3>${esc(t.nombre)}</h3><p class="small">${esc(t.carnet)} · Retirado</p>${tutorHistory(t)}</article>`).join('')}</details></section>`:''}`;
}
function tutorHistory(t){
 const history=sessionRequests(true).filter(x=>x.request.tutor===t.carnet);
 return `<details><summary>Disponibilidad e historial</summary><p class="small">${t.horarios.length?t.horarios.map(h=>date(h.fecha)+' · '+h.inicio+'–'+h.fin+' · '+(t.activo===false?'Retirado':h.disponible?'Disponible':'Reservado')).join('<br>'):'Sin horarios publicados.'}</p><p class="small">Tutorías finalizadas: ${history.length}</p><p class="small">${history.map(x=>esc(x.request.nombreEstudiante)+' · '+date(x.request.fecha)+' · '+esc(x.request.nombreCurso)).join('<br>')}</p><p class="small">Horas beca: ${t.horasBeca} · Remuneración acumulada: ${money(t.remuneracion)}</p></details>`;
}
function editTutor(q){
 const t=state.tutores.find(t=>t.carnet===q.get('tutor')&&t.activo!==false);
 if(!t)return empty('Perfil no disponible','Selecciona un tutor o profesor activo.',link('#tutores','Volver a tutores'));
 return heading('Editar '+(t.profesor?'profesor':'tutor'),'Actualiza sus datos y su forma de compensación.',link('#tutores','Volver a tutores','button secondary'))+
 `<section class="card form-panel">${form('editar-tutor',hidden('tutor',t.carnet)+field('carnet','Carné (identificador fijo)','text',t.carnet,'readonly')+field('nombre','Nombre completo','text',t.nombre,'maxlength="200"')+field('correo','Correo electrónico','email',t.correo,'maxlength="200"')+field('anio','Año de carrera','number',t.anio,'min="1" max="10"')+`<div class="field"><label class="check-label"><input type="checkbox" name="beca" id="beca" ${t.beca?'checked':''}>Recibe horas beca (desmarca para pago)</label></div>`+field('tarifa','Tarifa por hora en quetzales','number',t.beca?0:t.tarifa,'min="0" step="0.01" '+(t.beca?'readonly':''))+'<p class="muted small">Las nuevas solicitudes usarán esta compensación. Las reservas existentes conservan su precio y su forma de compensación; las horas beca y pagos ya acumulados se mantienen.</p>','Guardar cambios')}</section>`;
}
function students(){return heading('Estudiantes y acompañamiento','Consulta quiénes reciben tutorías y abre su progreso académico.')+`<section class="card"><details><summary>Registrar un estudiante</summary>${personForm('estudiante')}</details></section><div class="section-heading"><h2>Estudiantes registrados</h2><label class="check-label small"><input type="checkbox" id="solo-atendidos">Con tutorías</label></div><div id="lista-estudiantes">${studentTable(state.estudiantes)}</div>`;}
function studentTable(list){return list.length?`<div class="table-wrap"><table><thead><tr><th>Estudiante</th><th>Año</th><th>Atención</th><th>Progreso</th></tr></thead><tbody>${list.map(e=>`<tr><td><span class="row-title">${esc(e.nombre)}</span>${esc(e.carnet)} · ${esc(e.correo)}</td><td>${e.anio}</td><td>${e.recibeTutorias?'Con tutorías':'Sin tutorías confirmadas'}</td><td>${link('#progreso?estudiante='+encodeURIComponent(e.carnet),'Ver progreso','text-button')}</td></tr>`).join('')}</tbody></table></div>`:empty('No hay estudiantes en esta lista','Registra un estudiante o cambia el filtro.');}
function courses(){
 const retired=state.cursosEliminados||[];
 return heading('Cursos para seguir aprendiendo','Organiza la oferta académica disponible para las tutorías.')+`<div class="grid"><section class="card"><h2>Registrar curso</h2>${form('curso',field('codigo','Código del curso','text','','pattern="[A-Za-z0-9_-]+" maxlength="80"')+field('nombre','Nombre del curso','text','','maxlength="200"')+courseIconPicker(),'Guardar curso')}</section><section>${state.cursos.map(c=>`<article class="card course-card">${sticker(courseIcon(c))}<span class="course-code">${esc(c.codigo)}</span><h3>${esc(c.nombre)}</h3><p class="muted small">${state.tutores.filter(t=>t.activo!==false&&t.cursos.includes(c.codigo)).length} tutores autorizados</p><button type="button" class="danger" data-command="eliminar-curso" data-id="${esc(c.codigo)}">Eliminar curso</button></article>`).join('')||empty('No hay cursos activos','Registra un curso para ofrecer tutorías.')}</section></div>${retired.length?`<section class="card"><details><summary>Cursos retirados (${retired.length})</summary><p class="muted small">Ya no se ofrecen. Sus solicitudes e historial se conservan.</p>${retired.map(c=>`<p>${esc(c.codigo)} · ${esc(c.nombre)}</p>`).join('')}</details></section>`:''}`;
}
function demand(){
 const title=heading('Dónde hace falta más apoyo','Solicitudes por curso y tema. Las solicitudes canceladas no se incluyen.');
 const rows=state.demanda;
 if(!rows.length)return title+empty('Aún no hay cursos','Registra un curso para consultar su demanda.',link('#cursos','Registrar curso'));
 const total=rows.reduce((sum,c)=>sum+c.cantidad,0);
 return title+`<section class="card demand-card"><div class="section-heading"><h2 id="demanda-titulo">Demanda académica</h2><span class="badge">${rows.length} ${rows.length===1?'curso':'cursos'}</span></div><p class="muted small" id="demanda-ayuda">Incluye solicitudes pendientes, confirmadas y finalizadas. La cantidad junto a cada tema indica cuántas solicitudes lo mencionan.</p><div class="table-wrap" tabindex="0" role="region" aria-labelledby="demanda-titulo" aria-describedby="demanda-ayuda"><table class="demand-table"><caption>Solicitudes por curso y temas solicitados</caption><thead><tr><th scope="col">Código</th><th scope="col">Curso</th><th scope="col" class="demand-number">Solicitudes</th><th scope="col">Temas solicitados</th></tr></thead><tbody>${rows.map(c=>`<tr><td class="demand-code">${esc(c.codigo)}</td><th scope="row"><span class="demand-course">${icon(courseIcon(c))}<span>${esc(c.nombre)}${c.activo===false?' · Retirado':''}</span></span></th><td class="demand-number"><span class="demand-count">${c.cantidad}</span></td><td>${c.temas.length?`<ul class="demand-topics">${c.temas.map(t=>`<li><span>${esc(t.tema)}</span><span class="badge" aria-label="${t.cantidad} ${t.cantidad===1?'solicitud':'solicitudes'}">${t.cantidad}</span></li>`).join('')}</ul>`:'<span class="muted">Sin solicitudes</span>'}</td></tr>`).join('')}</tbody><tfoot><tr><th scope="row" colspan="2">Total de solicitudes</th><td class="demand-number"><strong>${total}</strong></td><td>Sin canceladas</td></tr></tfoot></table></div></section>`;
}
function progress(q){return heading('Cada sesión deja un avance','Consulta los contenidos, las notas y el siguiente tema de cada estudiante.')+`<div class="filterbar"><div class="field"><label for="progreso-estudiante">Estudiante</label><select id="progreso-estudiante"><option value="">Todos los estudiantes</option>${state.estudiantes.map(e=>`<option value="${esc(e.carnet)}" ${q.get('estudiante')===e.carnet?'selected':''}>${esc(e.nombre)}</option>`).join('')}</select></div></div><div id="lista-progreso">${history(q.get('estudiante')||'')}</div>`;}
function professor(q){if(currentTutor()?.activo===false)return heading('Perfil docente retirado','Tu acceso de coordinación sigue activo.')+empty('Ya no ofreces tutorías','El historial y la compensación se conservan. Puedes seguir administrando el sistema.');const tab=q.get('vista')||'solicitudes';let body;if(tab==='disponibilidad')body=availability();else if(tab==='compensacion')body=compensation();else body=heading('También puedes acompañar','Usa tu perfil de profesor para aceptar tutorías y registrar el seguimiento.')+requestList(mine());return `<div class="tabs">${[['solicitudes','Mis tutorías'],['disponibilidad','Mi disponibilidad'],['compensacion','Mi compensación']].map(([v,l])=>link('#profesor?vista='+v,l,v===tab?'active':'')).join('')}</div>`+body;}
function account(){return heading('Tu cuenta','Mantén tu acceso personal actualizado.')+`<section class="card account"><h2>Cambiar contraseña</h2>${form('clave',field('actual','Contraseña actual','password','','autocomplete="current-password"')+field('nueva','Nueva contraseña','password','','minlength="8" maxlength="128" autocomplete="new-password"'),'Actualizar contraseña')}</section>`;}
function help(){return heading('Una guía para empezar','Lo necesario para aprovechar tu espacio de tutorías.')+`<section class="card help"><h2>De la solicitud al siguiente tema</h2><ol><li><strong>El estudiante busca y solicita.</strong> Selecciona curso, tutor y horario. El precio total aparece antes de confirmar.</li><li><strong>El tutor acepta.</strong> La solicitud cambia de pendiente a confirmada y se crea la sesión.</li><li><strong>Se realiza la tutoría.</strong> El tutor registra temas, notas y un próximo tema opcional.</li><li><strong>Todos pueden consultar el seguimiento.</strong> El estudiante ve su historial, el tutor su compensación y coordinación el progreso.</li></ol><h2>Preguntas frecuentes</h2><p><strong>¿Puedo cancelar?</strong> Sí, mientras la solicitud esté pendiente o confirmada. El horario se libera.</p><p><strong>¿Y si estoy en último año?</strong> Usa Ayúdame a encontrar un tutor. Se buscará un tutor aprobado con horario compatible y, cuando sea necesario, un profesor.</p><p><strong>¿Mis datos se conservan al cerrar?</strong> Sí. Las operaciones confirmadas se guardan en la base de datos.</p><p><strong>¿Qué significan las horas beca?</strong> Son la compensación registrada para los tutores que tienen ese perfil. El cálculo del proyecto no sustituye la aprobación oficial de la universidad.</p></section>`;}
function render(){if(!state){login();return;}const {page,q}=route();let content;const allowed=new Set([...navigation().map(n=>n[0]),'ayuda','cuenta',...(state.usuario.rol==='estudiante'?['reservar']:['seguimiento']),...(state.usuario.rol==='coordinador'?['editar-tutor']:[])]);if(!allowed.has(page)){location.hash='inicio';return;}
switch(page){case'inicio':content=home();break;case'buscar':content=search();break;case'reservar':content=booking(q);break;case'solicitudes':content=requests();break;case'historial':content=heading('Un registro de lo que has aprendido','Temas, notas y siguientes pasos de las tutorías finalizadas.')+history();break;case'preguntas':content=questions();break;case'sesiones':content=sessions();break;case'seguimiento':content=followup(q);break;case'disponibilidad':content=availability();break;case'compensacion':content=compensation();break;case'tutores':content=tutors();break;case'editar-tutor':content=editTutor(q);break;case'estudiantes':content=students();break;case'cursos':content=courses();break;case'progreso':content=progress(q);break;case'demanda':content=demand();break;case'profesor':content=professor(q);break;case'cuenta':content=account();break;default:content=help();}
shell(content,page);if(page==='reservar'){document.querySelector('#curso').value=q.get('curso')||'';updateTutors(q.get('tutor')||'');}}
async function perform(action,data,element){
 const button=element?.querySelector('button[type=submit]')||element;
 if(button)button.disabled=true;
 try{await api(action==='clave'?'clave':'accion/'+action,data);await load();
 const destination=action==='editar-tutor'?'tutores':action==='solicitar'?'solicitudes':action==='finalizar'?(state.usuario.rol==='coordinador'?'profesor':'historial'):null;
 // La navegación renderiza una sola vez. Un segundo render cerraba formularios
 // que el usuario acababa de abrir después de guardar una solicitud.
 if(destination && location.hash!=='#'+destination)location.hash=destination;else render();
 notify(action==='eliminar-curso'?'Curso retirado de la oferta. El historial se conserva.':action==='eliminar-tutor'?'Perfil retirado de tutorías. El historial se conserva.':action==='clave'?'Contraseña actualizada.':action==='solicitar'?'Solicitud enviada. Tu horario está reservado.':'Cambio guardado correctamente.');
 }catch(e){
 if(e.status===401){const session=await api('sesion');csrf=session.csrf;login();notify(e.message);return;}
 if(action==='solicitar' && [400,409].includes(e.status)){
   // Actualizar la oferta sin borrar el tema ni los demás datos del formulario.
   try{await load();updateTutors(data.tutor);const horarios=document.querySelector('#horario');
     if([...horarios.options].some(o=>o.value===data.horario))horarios.value=data.horario;
     updateSummary();
   }catch{/* El error original permanece visible y se puede volver a intentar. */}
 }
 const box=element?.querySelector('.form-error');if(box){box.textContent=e.message;box.tabIndex=-1;box.focus();}else notify(e.message);
 }finally{if(button)button.disabled=false;}}
function confirmOperation(title,description,label,action){
 cancelAction=action;
 document.querySelector('#confirmar-titulo').textContent=title;
 document.querySelector('#confirmar-descripcion').textContent=description;
 document.querySelector('#cancelar-confirmado').textContent=label;
 document.querySelector('#conservar').textContent=title==='Cancelar esta tutoría'?'Conservar tutoría':'Volver';
 document.querySelector('#confirmacion').showModal();
}
document.addEventListener('submit',async e=>{const f=e.target;if(!(f instanceof HTMLFormElement))return;e.preventDefault();if(!f.reportValidity())return;const data=Object.fromEntries(new FormData(f));if(f.id==='login-form'){const btn=f.querySelector('button[type=submit]');btn.disabled=true;f.querySelector('.form-error').textContent='';try{await api('login',data);await load();if(location.hash!=='#inicio')location.hash='inicio';else render();}catch(err){f.querySelector('.form-error').textContent=err.message;}finally{btn.disabled=false;}}else if(f.id==='registro-form')await registerStudent(f,data);else if(f.dataset.action)await perform(f.dataset.action,data,f);});
document.addEventListener('click',async e=>{const btn=e.target.closest('button');if(!btn)return;
 if(btn.dataset.demo){const [rol,id]=btn.dataset.demo.split(',');document.querySelector('#rol').value=rol;document.querySelector('#carnet').value=id;document.querySelector('#clave').value='Tutor2026!';document.querySelector('#login-form button[type=submit]').focus();}
 if(btn.id==='logout'){try{await api('logout',{});location.hash='inicio';login();}catch(err){notify(err.message);}}
 if(btn.id==='alternativa'){const c=state.cursos.find(c=>c.codigo===document.querySelector('#curso').value);if(!c){document.querySelector('#recomendacion').textContent='Selecciona primero un curso.';return;}if(!c.alternativa){document.querySelector('#recomendacion').textContent='No hay un tutor con horario compatible. Consulta a coordinación.';return;}updateTutors(c.alternativa);const t=state.tutores.find(t=>t.carnet===c.alternativa);document.querySelector('#recomendacion').textContent='Te recomendamos a '+t.nombre+(t.profesor?', profesor autorizado.':', tutor autorizado para este curso.')+' Elige uno de sus horarios.';}
 if(btn.dataset.command==='cancelar')confirmOperation('Cancelar esta tutoría','La solicitud se cancelará y el horario volverá a estar disponible.','Sí, cancelar tutoría',()=>perform('cancelar',{solicitud:btn.dataset.id},btn));
 if(btn.dataset.command==='eliminar-curso'){
  const c=state.cursos.find(c=>c.codigo===btn.dataset.id);if(!c)return;
  confirmOperation('Eliminar curso','Se retirará «'+c.nombre+'» de la oferta. Se conservará su historial. Primero debes resolver sus solicitudes pendientes o confirmadas.','Sí, eliminar curso',()=>perform('eliminar-curso',{curso:c.codigo},btn));
 }
 if(btn.dataset.command==='eliminar-tutor'){
  const t=state.tutores.find(t=>t.carnet===btn.dataset.id);if(!t)return;
  confirmOperation('Quitar '+(t.profesor?'profesor':'tutor'),'Se retirará a «'+t.nombre+'» de la oferta de tutorías y se conservarán su historial y compensación. Primero debes resolver sus solicitudes pendientes o confirmadas.'+(t.profesor?' Su acceso de coordinación seguirá activo.':' Su acceso como tutor quedará deshabilitado.'),'Sí, quitar perfil',()=>perform('eliminar-tutor',{tutor:t.carnet},btn));
 }
 if(btn.dataset.command==='aceptar')await perform('aceptar',{solicitud:btn.dataset.id},btn);
 if(btn.id==='conservar')document.querySelector('#confirmacion').close();
 if(btn.id==='cancelar-confirmado'){document.querySelector('#confirmacion').close();if(cancelAction)await cancelAction();}
});
document.addEventListener('change',e=>{const el=e.target;
 if(el.id==='curso'&&document.querySelector('#reserva-form'))updateTutors();if(el.id==='tutor'&&document.querySelector('#reserva-form'))updateHours();if(el.id==='horario')updateSummary();
 if(el.id==='filtro-estado')document.querySelector('#lista-solicitudes').innerHTML=requestList(state.solicitudes.filter(r=>!el.value||r.estado===el.value));
 if(el.id==='solo-atendidos')document.querySelector('#lista-estudiantes').innerHTML=studentTable(state.estudiantes.filter(s=>!el.checked||s.recibeTutorias));
 if(el.id==='progreso-estudiante')document.querySelector('#lista-progreso').innerHTML=history(el.value);
 if(el.id==='beca'){const tariff=document.querySelector('#tarifa');if(el.checked){tariff.value='0';tariff.readOnly=true;}else tariff.readOnly=false;}
});
document.addEventListener('input',e=>{if(e.target.id==='buscar-curso'){const q=e.target.value.toLocaleLowerCase();const courses=state.cursos.filter(c=>(c.nombre+' '+c.codigo).toLocaleLowerCase().includes(q));document.querySelector('#lista-cursos').innerHTML=courses.length?courseCards(courses):empty('No encontramos ese curso','Prueba otra palabra o el código del curso.');}});
window.addEventListener('hashchange',async()=>{try{if(state)await load();render();window.scrollTo(0,0);document.querySelector('#contenido')?.focus({preventScroll:true});}catch(e){if(e.status===401){await init();}else notify('No se pudo actualizar: '+e.message);}});
async function init(){try{const s=await api('sesion');csrf=s.csrf;demo=s.demo;if(s.autenticado)await load();else state=null;render();}catch(e){app.innerHTML='<main class="app-error"><h1>No pudimos conectar</h1><p>'+esc(e.message)+'</p><button id="reintentar">Reintentar</button></main>';document.querySelector('#reintentar').onclick=init;}}
init();
