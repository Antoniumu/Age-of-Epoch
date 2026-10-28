import {chromium} from 'playwright-core';
import assert from 'node:assert/strict';
import {spawn} from 'node:child_process';
import {mkdir,writeFile} from 'node:fs/promises';
const server=spawn('python3',['-m','http.server','8080','--bind','127.0.0.1','--directory','../../../docs/airship/crew-preview']);
let browser;
try {
 await new Promise((resolve,reject)=>{server.stderr.on('data',()=>resolve());server.once('error',reject);setTimeout(resolve,1000);});
 browser=await chromium.launch({headless:true,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader']});
 const page=await browser.newPage({viewport:{width:1500,height:1100},deviceScaleFactor:1});
 const errors=[];page.on('pageerror',e=>errors.push(String(e)));page.on('console',m=>{if(m.type()==='error')errors.push(m.text());});
 await page.goto('http://127.0.0.1:8080/');
 await page.waitForFunction(()=>window.previewReady||window.previewError,null,{timeout:60000});
 const state=await page.evaluate(()=>({ready:window.previewReady,error:window.previewError,stats:window.previewStats}));
 assert.equal(state.ready,true,JSON.stringify(state));assert.equal(state.stats.textures,7);assert.equal(state.stats.characters,8);assert.equal(state.stats.orenScale,1.6);assert.equal(state.stats.slimModels,3);
 assert.equal(await page.locator('.card canvas').count(),8);
 const out='../../../docs/airship/crew-preview';
 await page.locator('#stage').screenshot({path:`${out}/lineup.png`});
 await page.screenshot({path:`${out}/preview-proof.png`,fullPage:true});
 await page.locator('#back').click();
 await page.screenshot({path:`${out}/back-proof.png`,fullPage:true});
 await page.setViewportSize({width:390,height:844});
 await page.screenshot({path:`${out}/mobile-proof.png`,fullPage:true});
 assert.deepEqual(errors,[]);
 await writeFile(`${out}/render-test.json`,JSON.stringify({state,errors,viewports:[[1500,1100],[390,844]],checks:['7 textures loaded','3 slim models','8 non-default models','Oren 1.6 scale','front/back controls clicked','desktop/mobile rendered','no browser errors']},null,2)+'\n');
 console.log(JSON.stringify(state));
}finally{await browser?.close();server.kill();}
