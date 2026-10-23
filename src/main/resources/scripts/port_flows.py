"""
Quick script for porting physics.toml into bitmap.bits
Might not handle everything, check and test all files produced with it!!
"""

import copy
import toml
from pathlib import Path
from typing import Any, NotRequired, TypeAlias, TypedDict

from utils.dirs import assets_dir
from utils.log import warn, err, info, log

Bit: TypeAlias = int
BitStr: TypeAlias = str
MapName: TypeAlias = str

class PhysFlowValues(TypedDict):
    flow_in: float
    flow_out: float
    gravity_in: NotRequired[float]
    gravity_out: NotRequired[float]
    smash_in: NotRequired[float]
    smash_out: NotRequired[float]
    smash_in_speed: NotRequired[float]
    smash_out_speed: NotRequired[float]

MappedBits = dict[MapName, list[Bit]]
MappedPhysFlows = dict[MapName, dict[BitStr, PhysFlowValues]]


next_drawer = 150
phys_flows: MappedPhysFlows = {}


def update_physics(path: Path):
    global phys_flows
    if not path.exists():
        warn(f"Physics is missing at path: {path}")
        return
    physics: dict[str, Any] = toml.loads(path.read_text())

    if "bits" not in physics.keys(): return {}
    bits: MappedPhysFlows = physics["bits"]

    phys_flows = copy.deepcopy(bits)

    physics["bits"] = None
    path.write_text(toml.dumps(physics))


def update_bitmap(path: Path):
    bot_id = path.parent.name
    def bot_info(msg): info(f"[{bot_id}] {msg}")
    def bot_warn(msg): warn(f"[{bot_id}] {msg}")

    if not path.exists():
        bot_warn(f"Bitmap is missing at path: {path}")
        return
    
    lines = path.read_text().splitlines()
    block_bits: MappedBits = { }
    block_entry_last_line = False
    for line_index, line in enumerate(lines):
        line_strip: str = line.strip()
        def line_warn(msg): warn(f"[{bot_id}] Line {line_index}: {msg}")
        def line_err(msg): err(f"[{bot_id}] Line {line_index}: {msg}")

        # Finding the block
        if line_strip.endswith('{'):
            block_line = line_strip.replace(',', '').replace('{', '').strip().split(' ')
            current_map = ""
            for i, token in enumerate(block_line):
                token = token.strip()
                if len(token) == 0: continue
                
                # Map
                if i % 2 == 0:
                    current_map = token
                    if current_map not in block_bits.keys():
                        block_bits[current_map] = []
                
                # Bit
                if i % 2 == 1:
                    bit: int = int(token.removesuffix("bd").removesuffix("td"))
                    if token.endswith("bd"):
                        bit = bit + next_drawer
                    if bit <= 0: continue
                    block_bits[current_map].append(bit)
            block_entry_last_line = True
            continue

        # Making the new flow statement
        def make_flow_line() -> str:
            found_flows: list[tuple[str, int, PhysFlowValues]] = []
            any_missing = False
            for map, bits in block_bits.items():
                flows = phys_flows.get(map, {})
                for bit in bits:
                    found = flows.get(str(bit))
                    if found is None:
                        line_warn(f"No flows are present in the physics.toml for '{map} {bit}'. Skipping")
                        any_missing = True
                    else:
                        found_flows.append((map, bit, found))

            if any_missing:
                return ""
            if len(found_flows) == 0:
                line_warn("No bits found in block")
                return ""

            preferred = found_flows[0]
            for entry in found_flows:
                if entry[0] == "rae":
                    preferred = entry
                    break

            for map, bit, other_flow in found_flows:
                if other_flow != preferred[2]:
                    line_warn(f"'{map} {bit}' has a different flow than '{preferred[0]} {preferred[1]}', using '{preferred[0]} {preferred[1]}'")
                    break

            # Building the string
            flow: PhysFlowValues = preferred[2]
            out: str = f"flow in:{flow["flow_in"]} out:{flow["flow_out"]}"

            if "gravity_in" in flow:
                out += f" gravity_in:{flow["gravity_in"]}"
            if "gravity_out" in flow:
                out += f" gravity_out:{flow["gravity_out"]}"

            if "smash_in" in flow:
                out += f" smash_in:{flow["smash_in"]}"
            if "smash_out" in flow:
                out += f" smash_out:{flow["smash_out"]}"
            if "smash_in_speed" in flow:
                out += f" smash_in_speed:{flow["smash_in_speed"]}"
            if "smash_out_speed" in flow:
                out += f" smash_out_speed:{flow["smash_out_speed"]}"
            
            return out.strip()

        # Finding the flow
        if line_strip.startswith("flow:"):
            new = make_flow_line()
            if len(new) == 0: continue
            lines[line_index] = (' ' * 4) + new
            bot_info(f"Found old flow '{line_strip}', replacing with '{new}'")
        elif block_entry_last_line:
            new = make_flow_line()
            if len(new) == 0: continue
            lines.insert(line_index, (' ' * 4) + new)
            block_entry_last_line = False
            bot_info(f"Adding new flow statement '{new}'")

    path.write_text('\n'.join(lines))


def convert_bot(dir: Path):
    if dir.name != "choo_choo": return  # test

    physics_file = dir / "physics.toml"
    update_physics(physics_file)

    bitmap_file = dir / "bitmap.bits"
    update_bitmap(bitmap_file)


for pack_dir in assets_dir.iterdir():
    if not pack_dir.is_dir():
        continue

    info(f"# Pack {pack_dir.name}")
    bots_dir = pack_dir / "showbiz" / "bots"
    if not bots_dir.exists():
        info(f"Pack {pack_dir.name} has no Showbiz addons\n")
        continue

    for bot_dir in bots_dir.iterdir():
        convert_bot(bot_dir)
    
    log("\n")
