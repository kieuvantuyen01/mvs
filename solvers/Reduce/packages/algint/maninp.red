module maninp;

% Author: James H. Davenport.

% Redistribution and use in source and binary forms, with or without
% modification, are permitted provided that the following conditions are met:
%
%    * Redistributions of source code must retain the relevant copyright
%      notice, this list of conditions and the following disclaimer.
%    * Redistributions in binary form must reproduce the above copyright
%      notice, this list of conditions and the following disclaimer in the
%      documentation and/or other materials provided with the distribution.
%
% THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
% AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
% THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
% PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNERS OR
% CONTRIBUTORS
% BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
% CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
% SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
% INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
% CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
% ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
% POSSIBILITY OF SUCH DAMAGE.
%


fluid '(intvar);

symbolic procedure findmaninparm places;
begin
  scalar sqrts,vars,u;
  sqrts:=sqrtsinplaces places;
loop:
  if null sqrts then return nil;
  vars:=getvariables simp argof car sqrts;
innerloop:
  if null vars
    then <<
      sqrts:=cdr sqrts;
      go to loop >>;
  u:=car vars;
  vars:=cdr vars;
  if u eq intvar
    then go to innerloop;
  if atom u
    then return u;
  if car u eq 'sqrt
    then << u:=simp argof u;
            vars:=varsinsf(numr u,varsinsf(denr u,vars));
            go to innerloop >>;
  interr "Unrecognised differentiation candidate"
  end;

endmodule;

end;


