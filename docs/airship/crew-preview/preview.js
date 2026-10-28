import { THREE as T, SkinObject, loadSkinToCanvas, loadImage, OrbitControls } from './vendor/viewer.js';
const views=[],models=[],textures=[];
let turning=false;
const status=document.querySelector('#status');
window.previewReady=false;
function view(canvas,wide=false){
 const renderer=new T.WebGLRenderer({canvas,alpha:true,antialias:true,preserveDrawingBuffer:true});
 renderer.setPixelRatio(Math.min(devicePixelRatio,2));
 renderer.outputColorSpace=T.SRGBColorSpace;
 const scene=new T.Scene();scene.add(new T.HemisphereLight(0xfff3dc,0x647d83,2));
 const light=new T.DirectionalLight(0xffe9c5,2);light.position.set(-30,60,70);scene.add(light);
 const camera=new T.PerspectiveCamera(35,1,.1,1000);
 camera.position.set(0,wide?40:31,wide?235:110);
 const controls=new OrbitControls(camera,canvas);controls.target.set(0,wide?23:27,0);controls.enablePan=false;controls.minDistance=wide?80:40;controls.maxDistance=wide?450:180;controls.enableDamping=true;controls.update();
 const root=new T.Group();scene.add(root);
 const v={renderer,scene,camera,controls,canvas,root,wide};views.push(v);return v;
}
function floor(root,x=0,r=10){
 const m=new T.Mesh(new T.CylinderGeometry(r,r,.18,48),new T.MeshStandardMaterial({color:0x36494a,roughness:.9}));m.position.set(x,-.2,0);root.add(m);
}
function bird(){
 const g=new T.Group();
 const brass=new T.MeshStandardMaterial({color:0xb98a45,metalness:.75,roughness:.35});
 const dark=new T.MeshStandardMaterial({color:0x344e50,metalness:.65,roughness:.5});
 const gold=new T.MeshStandardMaterial({color:0xe7bd6b,metalness:.75,roughness:.28});
 const eye=new T.MeshStandardMaterial({color:0xffb354,emissive:0xff9b21,emissiveIntensity:1.8});
 function box(w,h,d,x,y,z,m=brass,parent=g){const a=new T.Mesh(new T.BoxGeometry(w,h,d),m);a.position.set(x,y,z);parent.add(a);return a;}
 box(4.2,4.5,3.7,0,4.5,0);box(4.6,3.7,4.2,0,8.1,.6);box(3.5,2.4,.4,0,4.8,2.05,dark);
 box(.7,.9,.35,-1.2,8.5,2.85,eye);box(.7,.9,.35,1.2,8.5,2.85,eye);
 const beak=box(1.2,1.1,1.6,0,7.7,3,gold);beak.rotation.x=-.2;
 for(const s of [-1,1]){
  box(.55,1.9,.55,s*1.15,1.6,.1,dark);box(1.3,.4,2,s*1.15,.6,.7,gold);
  const wing=new T.Group();wing.position.set(s*2.2,5.3,-.4);wing.rotation.z=-s*.18;g.add(wing);
  for(let i=0;i<3;i++){const feather=box(.6,3.8-i*.45,1.15, s*(.25+i*.5),-i*.25,-i*.38,i%2?gold:brass,wing);feather.rotation.x=-.18;}
  box(.45,.7,.4,s*1.2,10.3,.1,gold);
 }
 for(let i=-1;i<=1;i++){const tail=box(.75,.5,3.5,i*.7,3.5,-3.1,i===0?dark:gold);tail.rotation.x=.3;tail.rotation.y=i*.12;}
 g.userData.kind='original-bird-prototype';return g;
}
function label(root,text,x){
 const c=document.createElement('canvas');c.width=512;c.height=96;const ctx=c.getContext('2d');ctx.fillStyle='#eadfc8';ctx.font='bold 32px sans-serif';ctx.textAlign='center';ctx.fillText(text,256,50);
 const texture=new T.CanvasTexture(c);const sprite=new T.Sprite(new T.SpriteMaterial({map:texture,depthTest:false,transparent:true}));sprite.scale.set(27,5.1,1);sprite.position.set(x,-5,4);root.add(sprite);
}
function avatar(texture,scale,model){const skin=new SkinObject();skin.map=texture;skin.modelType=model==='slim'?'slim':'default';skin.scale.setScalar(scale);skin.position.y=24*scale;skin.rotation.y=.28;skin.rightArm.rotation.z=.035;skin.leftArm.rotation.z=-.035;skin.head.rotation.y=-.07;return skin;}
try{
 const response=await fetch('./alternatives.json');if(!response.ok)throw Error('Нет реестра текстур');const people=await response.json();
 const stage=view(document.querySelector('#stage'),true);
 const loaded=await Promise.all(people.map(async p=>{const c=document.createElement('canvas');loadSkinToCanvas(c,await loadImage(p.skin));const texture=new T.CanvasTexture(c);texture.magFilter=T.NearestFilter;texture.minFilter=T.NearestFilter;texture.colorSpace=T.SRGBColorSpace;textures.push(texture);return {...p,texture};}));
 const all=[...loaded,{id:'tik',name:'Тик',role:'Маленькая механическая птица • собственный эскиз',scale:1}];
 all.forEach((p,i)=>{
  const article=document.createElement('article');article.className='card';
  article.innerHTML=`<canvas aria-label="3D-модель: ${p.name}"></canvas><div class="info"><span class="tag">${p.id==='tik'?'СОБСТВЕННЫЙ ЭСКИЗ':p.scale===1.6?'ИНТЕРНЕТ-ТЕКСТУРА / 1,6×':'ИНТЕРНЕТ-ТЕКСТУРА / 1×'}</span><h2>${p.name}</h2><p>${p.role}</p>${p.source?`<a href="${p.source}" target="_blank" rel="noopener">Оригинал и источник ↗</a>`:'Не найденный готовый ассет; отдельная птичья геометрия.'}</div>`;
  document.querySelector('#grid').append(article);const v=view(article.querySelector('canvas'));
  const m=p.texture?avatar(p.texture,p.scale,p.model):bird();v.root.add(m);floor(v.root,0,p.id==='tik'?5:p.scale*9);models.push(m);
  const x=-109+i*31;
  const other=p.texture?avatar(p.texture,p.scale,p.model):bird();other.position.x=x;stage.root.add(other);floor(stage.root,x,p.id==='tik'?5:p.scale*8);label(stage.root,p.name,x);models.push(other);
 });
 status.textContent='Загружено: 7 адаптаций лицензированных текстур + 1 собственный эскиз птицы. Все восемь моделей доступны для вращения.';
 window.previewStats={textures:textures.length,characters:all.length,slimModels:loaded.filter(p=>p.model==='slim').length,orenScale:loaded.find(p=>p.id==='oren').scale,birdGeometry:'non-humanoid original prototype'};
 window.previewReady=true;
 document.querySelector('#front').onclick=()=>models.forEach(m=>m.rotation.y=.28);
 document.querySelector('#back').onclick=()=>models.forEach(m=>m.rotation.y=Math.PI+.28);
 document.querySelector('#turn').onclick=e=>{turning=!turning;e.target.textContent=`Вращать: ${turning?'вкл.':'выкл.'}`;};
 let last=performance.now();function frame(now){const dt=Math.min((now-last)/1000,.1);last=now;if(turning)models.forEach(m=>m.rotation.y+=dt*.35);
  for(const v of views){const w=v.canvas.clientWidth,h=v.canvas.clientHeight;if(v.canvas.width!==Math.round(w*v.renderer.getPixelRatio())||v.canvas.height!==Math.round(h*v.renderer.getPixelRatio())){v.renderer.setSize(w,h,false);v.camera.aspect=w/h;if(v.wide){v.camera.position.set(0,40,Math.max(115,260/(2*Math.tan(35*Math.PI/360)*(w/h))));}v.camera.updateProjectionMatrix();}v.controls.update();v.renderer.render(v.scene,v.camera);}requestAnimationFrame(frame);}
 requestAnimationFrame(frame);
}catch(error){console.error(error);status.classList.add('error');status.textContent='3D-примерка не загрузилась: '+error.message+'. Требуется браузер с WebGL и HTTP-сервер (не file://).';window.previewError=String(error);}
